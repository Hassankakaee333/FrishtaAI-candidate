package ai.hassan.app.phoneagent

import android.content.Context
import ai.hassan.app.cloud.HassanCloudApi
import ai.hassan.app.conversation.ConversationSettingsStore
import java.io.File
import java.util.Base64
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Cloud inbox/outbox for the phone agent.
 *
 * Commands live under phone-agent/inbox/<id>.json in a dedicated Hassan Cloud
 * project workspace. Results are written to phone-agent/outbox/<id>.json.
 * The access token never leaves the device.
 */
class PhoneAgentCloudBridge(
    context: Context,
    private val settingsStore: ConversationSettingsStore,
    private val cloudApi: HassanCloudApi,
    private val service: HassanAccessibilityService,
) {
    private val appContext = context.applicationContext
    private val preferences = PhoneAgentPreferences(appContext)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val pendingDir = File(appContext.filesDir, "phone-agent/pending-results").apply { mkdirs() }
    private var projectId: String? = null
    private var lastHeartbeatAt = 0L

    suspend fun runLoop() {
        while (currentCoroutineContext().isActive) {
            if (!preferences.isEnabled()) {
                delay(3_000)
                continue
            }
            val settings = settingsStore.read()
            if (settings.cloudBaseUrl.isBlank() || settings.accessToken.isBlank()) {
                delay(8_000)
                continue
            }
            val activeProjectId = runCatching {
                ensureProject(settings.cloudBaseUrl, settings.accessToken)
            }.getOrNull()
            if (activeProjectId == null) {
                delay(10_000)
                continue
            }

            runCatching {
                flushPendingResults(settings.cloudBaseUrl, settings.accessToken, activeProjectId)
                publishHeartbeatIfDue(settings.cloudBaseUrl, settings.accessToken, activeProjectId)
                pollCommands(settings.cloudBaseUrl, settings.accessToken, activeProjectId)
            }
            delay(4_000)
        }
    }

    private suspend fun ensureProject(baseUrl: String, token: String): String {
        projectId?.let { return it }
        val existing = cloudApi.listProjects(baseUrl, token).getOrThrow()
            .firstOrNull { it.name == PROJECT_NAME }
        val resolved = existing ?: cloudApi.createProject(
            baseUrl = baseUrl,
            token = token,
            name = PROJECT_NAME,
            description = "Persistent command/results workspace for Hassan Phone Agent",
        ).getOrThrow()
        projectId = resolved.id
        return resolved.id
    }

    private suspend fun pollCommands(baseUrl: String, token: String, projectId: String) {
        val processed = preferences.processedPaths()
        val files = cloudApi.listWorkspaceFiles(baseUrl, token, projectId).getOrThrow()
            .asSequence()
            .filter { it.path.startsWith(INBOX_PREFIX) && it.path.endsWith(".json") }
            .filterNot { processed.contains(it.path) }
            .sortedBy { it.updated_at }
            .take(MAX_COMMANDS_PER_POLL)
            .toList()

        for (file in files) {
            val parsed = runCatching {
                val content = cloudApi.getWorkspaceFile(baseUrl, token, projectId, file.path).getOrThrow()
                val decoded = Base64.getDecoder().decode(content.content_base64).toString(Charsets.UTF_8)
                json.decodeFromString(PhoneAgentCommand.serializer(), decoded)
            }
            if (parsed.isFailure) {
                val id = file.path.substringAfterLast('/').removeSuffix(".json")
                val bad = PhoneAgentResult(
                    id = id,
                    status = "INVALID",
                    message = "تعذر قراءة الأمر: ${parsed.exceptionOrNull()?.message}",
                    activePackage = service.activePackage(),
                )
                preferences.markProcessed(file.path)
                queueOrSendResult(baseUrl, token, projectId, bad)
                continue
            }

            val command = parsed.getOrThrow()
            val result = when {
                command.expiresAtEpochMs > 0 && System.currentTimeMillis() > command.expiresAtEpochMs ->
                    PhoneAgentResult(command.id, "EXPIRED", "انتهت صلاحية الأمر", service.activePackage())
                command.requiresConfirmation ->
                    PhoneAgentResult(
                        command.id,
                        "NEEDS_CONFIRMATION",
                        "الأمر مصنف حساسًا ويتطلب موافقة محلية قبل التنفيذ",
                        service.activePackage(),
                    )
                command.action.uppercase() !in PhoneAgentActions.supported ->
                    PhoneAgentResult(command.id, "UNSUPPORTED", "أمر غير مدعوم: ${command.action}", service.activePackage())
                else -> executeCommand(baseUrl, token, projectId, command)
            }

            // Mark before publishing the result so a transient network failure cannot
            // cause a state-changing UI command to execute twice.
            preferences.markProcessed(file.path)
            queueOrSendResult(baseUrl, token, projectId, result)
        }
    }

    private suspend fun executeCommand(
        baseUrl: String,
        token: String,
        projectId: String,
        command: PhoneAgentCommand,
    ): PhoneAgentResult {
        val execution = service.execute(command.copy(action = command.action.uppercase()))
        var artifactId: String? = null
        execution.screenshotFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                artifactId = cloudApi.uploadFile(
                    baseUrl = baseUrl,
                    token = token,
                    file = file,
                    mimeType = "image/png",
                    projectId = projectId,
                ).getOrNull()?.id
                file.delete()
            }
        }
        return PhoneAgentResult(
            id = command.id,
            status = if (execution.ok) "COMPLETED" else "FAILED",
            message = execution.message,
            activePackage = service.activePackage(),
            uiTree = execution.uiTree,
            screenshotArtifactId = artifactId,
        )
    }

    private suspend fun queueOrSendResult(
        baseUrl: String,
        token: String,
        projectId: String,
        result: PhoneAgentResult,
    ) {
        val payload = json.encodeToString(result)
        val sent = cloudApi.putWorkspaceFile(
            baseUrl,
            token,
            projectId,
            "$OUTBOX_PREFIX${safeId(result.id)}.json",
            payload.toByteArray(),
        ).isSuccess
        if (!sent) {
            File(pendingDir, "${safeId(result.id)}.json").writeText(payload)
        }
    }

    private suspend fun flushPendingResults(baseUrl: String, token: String, projectId: String) {
        pendingDir.listFiles { file -> file.extension == "json" }.orEmpty().forEach { file ->
            val result = runCatching {
                json.decodeFromString(PhoneAgentResult.serializer(), file.readText())
            }.getOrNull() ?: return@forEach
            val sent = cloudApi.putWorkspaceFile(
                baseUrl,
                token,
                projectId,
                "$OUTBOX_PREFIX${safeId(result.id)}.json",
                file.readBytes(),
            ).isSuccess
            if (sent) file.delete()
        }
    }

    private suspend fun publishHeartbeatIfDue(baseUrl: String, token: String, projectId: String) {
        val now = System.currentTimeMillis()
        if (now - lastHeartbeatAt < HEARTBEAT_INTERVAL_MS) return
        val activePackageJson = json.encodeToString(service.activePackage())
        val payload = """{"online":true,"enabled":${preferences.isEnabled()},"activePackage":$activePackageJson,"at":$now}"""
        cloudApi.putWorkspaceFile(
            baseUrl,
            token,
            projectId,
            HEARTBEAT_PATH,
            payload.toByteArray(),
        ).getOrThrow()
        lastHeartbeatAt = now
    }

    private fun safeId(id: String): String = id.replace(Regex("[^A-Za-z0-9._-]"), "_").take(96)

    companion object {
        const val PROJECT_NAME = "Hassan Phone Agent"
        const val INBOX_PREFIX = "phone-agent/inbox/"
        const val OUTBOX_PREFIX = "phone-agent/outbox/"
        const val HEARTBEAT_PATH = "phone-agent/heartbeat.json"
        private const val MAX_COMMANDS_PER_POLL = 12
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
    }
}
