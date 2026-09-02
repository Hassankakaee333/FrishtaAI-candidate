package ai.hassan.app.cloud

import ai.hassan.app.conversation.ConversationSettingsStore
import ai.hassan.app.data.CloudJobEntity
import ai.hassan.app.data.HassanDatabase
import java.util.UUID

class CloudJobOrchestratorImpl(
    private val settingsStore: ConversationSettingsStore,
    private val cloudApi: HassanCloudApi,
    private val database: HassanDatabase,
) : CloudTaskOrchestrator {
    override val isConfigured: Boolean
        get() = settingsStore.isCloudConfigured()

    override suspend fun submitTask(
        projectId: String,
        conversationId: String?,
        goal: String,
        jobType: String,
    ): Result<CloudTaskRef> {
        val settings = settingsStore.read()
        if (!settingsStore.isCloudConfigured()) {
            return Result.failure(
                UnsupportedOperationException("لم يتم إعداد Hassan Cloud بعد."),
            )
        }
        val idempotencyKey = listOfNotNull(projectId, conversationId, goal, jobType).joinToString(":")
        return cloudApi.createJob(
            baseUrl = settings.cloudBaseUrl,
            token = settings.accessToken,
            projectId = projectId,
            conversationId = conversationId,
            goal = goal,
            jobType = jobType,
            idempotencyKey = idempotencyKey,
        ).map { dto ->
            val entity = CloudJobEntity(
                id = dto.id,
                cloudProjectId = dto.project_id,
                conversationId = dto.conversation_id,
                goal = dto.goal,
                state = dto.state,
                resultSummary = dto.result_summary,
                log = dto.log,
                createdAt = dto.created_at,
                updatedAt = dto.updated_at,
            )
            database.cloudJobDao().upsert(entity)
            CloudTaskRef(
                taskId = dto.id,
                projectId = dto.project_id,
                conversationId = dto.conversation_id,
                goal = dto.goal,
                state = mapState(dto.state),
                updatedAt = dto.updated_at,
            )
        }
    }

    override suspend fun getTask(taskId: String): CloudTaskRef? {
        val local = database.cloudJobDao().getById(taskId) ?: return null
        if (!isConfigured) return local.toRef()
        val settings = settingsStore.read()
        cloudApi.getJob(settings.cloudBaseUrl, settings.accessToken, taskId)
            .onSuccess { dto ->
                database.cloudJobDao().upsert(
                    CloudJobEntity(
                        id = dto.id,
                        cloudProjectId = dto.project_id,
                        conversationId = dto.conversation_id,
                        goal = dto.goal,
                        state = dto.state,
                        resultSummary = dto.result_summary,
                        log = dto.log,
                        createdAt = dto.created_at,
                        updatedAt = dto.updated_at,
                    ),
                )
            }
        return database.cloudJobDao().getById(taskId)?.toRef()
    }

    override suspend fun cancelTask(taskId: String): Result<Unit> {
        if (!isConfigured) {
            return Result.failure(UnsupportedOperationException("لم يتم إعداد Hassan Cloud بعد."))
        }
        val settings = settingsStore.read()
        return cloudApi.cancelJob(settings.cloudBaseUrl, settings.accessToken, taskId)
            .map { dto ->
                database.cloudJobDao().upsert(
                    CloudJobEntity(
                        id = dto.id,
                        cloudProjectId = dto.project_id,
                        conversationId = dto.conversation_id,
                        goal = dto.goal,
                        state = dto.state,
                        resultSummary = dto.result_summary,
                        log = dto.log,
                        createdAt = dto.created_at,
                        updatedAt = dto.updated_at,
                    ),
                )
            }
    }

    suspend fun syncJobs(): Int {
        if (!isConfigured) return 0
        val settings = settingsStore.read()
        val jobs = cloudApi.listJobs(settings.cloudBaseUrl, settings.accessToken).getOrNull() ?: return 0
        jobs.forEach { dto ->
            database.cloudJobDao().upsert(
                CloudJobEntity(
                    id = dto.id,
                    cloudProjectId = dto.project_id,
                    conversationId = dto.conversation_id,
                    goal = dto.goal,
                    state = dto.state,
                    resultSummary = dto.result_summary,
                    log = dto.log,
                    createdAt = dto.created_at,
                    updatedAt = dto.updated_at,
                ),
            )
        }
        return jobs.size
    }

    suspend fun syncArtifacts(): Int {
        if (!isConfigured) return 0
        val settings = settingsStore.read()
        val artifacts = cloudApi.listArtifacts(settings.cloudBaseUrl, settings.accessToken).getOrNull() ?: return 0
        artifacts.forEach { dto ->
            database.artifactDao().upsert(
                ai.hassan.app.data.ArtifactEntity(
                    id = dto.id,
                    projectId = dto.project_id,
                    jobId = dto.job_id,
                    conversationId = dto.conversation_id,
                    name = dto.name,
                    mimeType = dto.mime_type,
                    sizeBytes = dto.size_bytes,
                    localPath = null,
                    remoteUrl = "${settings.cloudBaseUrl.trimEnd('/')}/v1/files/${dto.id}",
                    createdAt = dto.created_at,
                ),
            )
        }
        return artifacts.size
    }

    suspend fun syncProjects(): Int {
        if (!isConfigured) return 0
        val settings = settingsStore.read()
        val projects = cloudApi.listProjects(settings.cloudBaseUrl, settings.accessToken).getOrNull() ?: return 0
        projects.forEach { dto ->
            database.projectDao().insert(
                ai.hassan.app.data.ProjectEntity(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description.ifBlank { "Hassan Cloud project" },
                    createdAt = dto.created_at,
                ),
            )
        }
        return projects.size
    }

    suspend fun listWorkspaceFiles(projectId: String): List<CloudWorkspaceFileDto> {
        if (!isConfigured) return emptyList()
        val settings = settingsStore.read()
        return cloudApi.listWorkspaceFiles(settings.cloudBaseUrl, settings.accessToken, projectId).getOrNull().orEmpty()
    }

    suspend fun readWorkspaceFile(projectId: String, path: String): Result<String> {
        if (!isConfigured) {
            return Result.failure(IllegalStateException("Hassan Cloud غير مُعد"))
        }
        val settings = settingsStore.read()
        return cloudApi.getWorkspaceFile(settings.cloudBaseUrl, settings.accessToken, projectId, path).map { dto ->
            java.util.Base64.getDecoder().decode(dto.content_base64).decodeToString()
        }
    }

    suspend fun syncAll(): Pair<Int, Int> {
        syncProjects()
        val jobs = syncJobs()
        val artifacts = syncArtifacts()
        return jobs to artifacts
    }

    private fun mapState(raw: String): CloudTaskState =
        runCatching { CloudTaskState.valueOf(raw) }.getOrDefault(CloudTaskState.QUEUED)

    private fun CloudJobEntity.toRef() = CloudTaskRef(
        taskId = id,
        projectId = cloudProjectId,
        conversationId = conversationId,
        goal = goal,
        state = mapState(state),
        updatedAt = updatedAt,
    )
}
