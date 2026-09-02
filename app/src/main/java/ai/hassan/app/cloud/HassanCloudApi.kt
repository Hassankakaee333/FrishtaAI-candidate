package ai.hassan.app.cloud

import ai.hassan.app.conversation.CloudChatMessageDto
import ai.hassan.app.conversation.CloudChatRequest
import ai.hassan.app.conversation.CloudChatResponse
import ai.hassan.app.conversation.ConversationMessage
import ai.hassan.app.conversation.ConversationResult
import ai.hassan.app.conversation.LocalHassanChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLEncoder
import java.util.Base64

/** Neon/HTTP often returns BIGINT fields as JSON strings. */
object FlexibleLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeLong()
        val element = jsonDecoder.decodeJsonElement()
        val primitive = element as? JsonPrimitive ?: return 0L
        return primitive.longOrNull ?: primitive.content.toLongOrNull() ?: 0L
    }

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }
}

@Serializable
data class CloudHealthResponse(
    val status: String,
    val service: String? = null,
    val openai_configured: Boolean = false,
)

@Serializable
data class CloudProjectDto(
    val id: String,
    val name: String,
    val description: String = "",
    val created_at: Long = 0,
    val updated_at: Long = 0,
)

@Serializable
data class CloudProjectCreateRequest(
    val name: String,
    val description: String = "",
)

@Serializable
data class CloudJobDto(
    val id: String,
    val project_id: String,
    val conversation_id: String? = null,
    val goal: String,
    val state: String,
    val result_summary: String? = null,
    val log: String = "",
    val created_at: Long = 0,
    val updated_at: Long = 0,
)

@Serializable
data class CloudJobCreateRequest(
    val project_id: String,
    val conversation_id: String? = null,
    val goal: String,
    val job_type: String = "general",
    val idempotency_key: String? = null,
)

@Serializable
data class CloudArtifactDto(
    val id: String,
    val project_id: String? = null,
    val job_id: String? = null,
    val conversation_id: String? = null,
    val name: String,
    val mime_type: String,
    val size_bytes: Long = 0,
    val created_at: Long = 0,
)

@Serializable
data class CloudWorkspaceFileDto(
    val path: String,
    @Serializable(with = FlexibleLongSerializer::class)
    val size_bytes: Long = 0,
    val sha256: String = "",
    @Serializable(with = FlexibleLongSerializer::class)
    val updated_at: Long = 0,
)

@Serializable
data class CloudWorkspaceFileContentDto(
    val path: String,
    val content_base64: String,
    @Serializable(with = FlexibleLongSerializer::class)
    val size_bytes: Long = 0,
    val sha256: String = "",
    @Serializable(with = FlexibleLongSerializer::class)
    val updated_at: Long = 0,
)

@Serializable
data class CloudWorkspaceFilePutRequest(
    val path: String,
    val content_base64: String,
)

@Serializable
data class CloudProjectWorkspaceDto(
    val project: CloudProjectDto,
    val conversations: List<CloudProjectConversationDto> = emptyList(),
    val jobs: List<CloudJobDto> = emptyList(),
    val artifacts: List<CloudArtifactDto> = emptyList(),
)

@Serializable
data class CloudProjectConversationDto(
    val id: String,
    val project_id: String? = null,
    val title: String = "",
    val created_at: Long = 0,
    val updated_at: Long = 0,
)

/**
 * Unified Hassan Cloud HTTP client — chat, projects, jobs, artifacts.
 */
class HassanCloudApi(
    private val httpClient: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun health(baseUrl: String): Result<CloudHealthResponse> = withContext(Dispatchers.IO) {
        getJson("$baseUrl/v1/health", token = null, CloudHealthResponse.serializer())
    }

    suspend fun verifyAuth(baseUrl: String, token: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/v1/auth/verify")
                .header("Authorization", "Bearer $token")
                .post("".toRequestBody(JSON_MEDIA))
                .build()
            httpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Auth failed: ${response.code}" }
            }
        }
    }

    suspend fun chat(
        baseUrl: String,
        accessToken: String,
        provider: String,
        messages: List<ConversationMessage>,
        conversationId: String? = null,
    ): ConversationResult = withContext(Dispatchers.IO) {
        val normalizedBase = baseUrl.trimEnd('/')
        if (normalizedBase.isBlank() || accessToken.isBlank()) {
            return@withContext ConversationResult.NotConfigured(
                message = "لم يتم إعداد مزود المحادثة بعد. افتح الإعدادات وأدخل رابط Hassan Cloud ورمز الوصول.",
            )
        }
        runCatching {
            val payload = CloudChatRequest(
                provider = provider,
                messages = messages.map { CloudChatMessageDto(role = it.role, content = it.content) },
                conversationId = conversationId,
            )
            val body = json.encodeToString(payload).toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url("$normalizedBase/v1/chat")
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .post(body)
                .build()
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body.string()
                if (!response.isSuccessful) {
                    return@runCatching ConversationResult.Error(
                        message = "فشل الاتصال بـ Hassan Cloud (${response.code}): ${raw.take(180)}",
                    )
                }
                val parsed = json.decodeFromString<CloudChatResponse>(raw)
                if (parsed.answer.isBlank()) {
                    ConversationResult.Error(message = "رد فارغ من Hassan Cloud.")
                } else if (
                    parsed.answer.contains("حالة Chat: NOT_CONFIGURED")
                ) {
                    ConversationResult.Success(
                        answer = LocalHassanChat.reply(
                            userMessage = messages.lastOrNull()?.content.orEmpty(),
                            history = messages.dropLast(1),
                            providerId = provider,
                        ),
                        providerId = LocalHassanChat.normalizeProvider(provider),
                    )
                } else {
                    // Keep server answer (real LLM, weather, or provider persona).
                    val resolvedProvider = when {
                        !parsed.provider.isNullOrBlank() -> parsed.provider
                        parsed.status.equals("OK", ignoreCase = true) -> provider
                        else -> LocalHassanChat.normalizeProvider(parsed.provider ?: provider)
                    }
                    ConversationResult.Success(
                        answer = parsed.answer.trim(),
                        providerId = LocalHassanChat.normalizeProvider(resolvedProvider),
                    )
                }
            }
        }.getOrElse {
            ConversationResult.Error(message = "تعذر الوصول إلى Hassan Cloud: ${it.message ?: "خطأ شبكة"}")
        }
    }

    suspend fun listProjects(baseUrl: String, token: String): Result<List<CloudProjectDto>> =
        withContext(Dispatchers.IO) {
            getJsonList("$baseUrl/v1/projects", token, CloudProjectDto.serializer())
        }

    suspend fun createProject(
        baseUrl: String,
        token: String,
        name: String,
        description: String = "",
    ): Result<CloudProjectDto> = withContext(Dispatchers.IO) {
        postJson(
            "$baseUrl/v1/projects",
            token,
            CloudProjectCreateRequest(name, description),
            CloudProjectDto.serializer(),
        )
    }

    suspend fun createJob(
        baseUrl: String,
        token: String,
        projectId: String,
        conversationId: String?,
        goal: String,
        jobType: String = "general",
        idempotencyKey: String? = null,
    ): Result<CloudJobDto> = withContext(Dispatchers.IO) {
        postJson(
            "$baseUrl/v1/jobs",
            token,
            CloudJobCreateRequest(projectId, conversationId, goal, jobType, idempotencyKey),
            CloudJobDto.serializer(),
        )
    }

    suspend fun cancelJob(baseUrl: String, token: String, jobId: String): Result<CloudJobDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("${baseUrl.trimEnd('/')}/v1/jobs/$jobId/cancel")
                    .header("Authorization", "Bearer $token")
                    .post("{}".toRequestBody(JSON_MEDIA))
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    val raw = response.body.string()
                    check(response.isSuccessful) { "HTTP ${response.code}: ${raw.take(120)}" }
                    json.decodeFromString(CloudJobDto.serializer(), raw)
                }
            }
        }

    suspend fun getJob(baseUrl: String, token: String, jobId: String): Result<CloudJobDto> =
        withContext(Dispatchers.IO) {
            getJson("$baseUrl/v1/jobs/$jobId", token, CloudJobDto.serializer())
        }

    suspend fun listJobs(baseUrl: String, token: String): Result<List<CloudJobDto>> =
        withContext(Dispatchers.IO) {
            getJsonList("$baseUrl/v1/jobs", token, CloudJobDto.serializer())
        }

    suspend fun listArtifacts(
        baseUrl: String,
        token: String,
        projectId: String? = null,
    ): Result<List<CloudArtifactDto>> = withContext(Dispatchers.IO) {
        val url = if (projectId.isNullOrBlank()) {
            "$baseUrl/v1/artifacts"
        } else {
            "$baseUrl/v1/artifacts?project_id=$projectId"
        }
        getJsonList(url, token, CloudArtifactDto.serializer())
    }

    suspend fun listWorkspaceFiles(
        baseUrl: String,
        token: String,
        projectId: String,
    ): Result<List<CloudWorkspaceFileDto>> = withContext(Dispatchers.IO) {
        getJsonList(
            "${baseUrl.trimEnd('/')}/v1/projects/$projectId/files",
            token,
            CloudWorkspaceFileDto.serializer(),
        )
    }

    suspend fun getWorkspaceFile(
        baseUrl: String,
        token: String,
        projectId: String,
        path: String,
    ): Result<CloudWorkspaceFileContentDto> = withContext(Dispatchers.IO) {
        val encodedPath = URLEncoder.encode(path, "UTF-8")
        getJson(
            "${baseUrl.trimEnd('/')}/v1/projects/$projectId/file?path=$encodedPath",
            token,
            CloudWorkspaceFileContentDto.serializer(),
        )
    }

    suspend fun putWorkspaceFile(
        baseUrl: String,
        token: String,
        projectId: String,
        path: String,
        data: ByteArray,
    ): Result<CloudWorkspaceFileDto> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = CloudWorkspaceFilePutRequest(path, Base64.getEncoder().encodeToString(data))
            val body = json.encodeToString(payload).toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/v1/projects/$projectId/files")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .put(body)
                .build()
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body.string()
                check(response.isSuccessful) { "HTTP ${response.code}: ${raw.take(120)}" }
                json.decodeFromString(CloudWorkspaceFileDto.serializer(), raw)
            }
        }
    }

    suspend fun uploadFile(
        baseUrl: String,
        token: String,
        file: File,
        mimeType: String,
        projectId: String? = null,
        jobId: String? = null,
        conversationId: String? = null,
    ): Result<CloudArtifactDto> = withContext(Dispatchers.IO) {
        runCatching {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody(mimeType.toMediaType()),
                )
            if (!projectId.isNullOrBlank()) body.addFormDataPart("project_id", projectId)
            if (!jobId.isNullOrBlank()) body.addFormDataPart("job_id", jobId)
            if (!conversationId.isNullOrBlank()) body.addFormDataPart("conversation_id", conversationId)
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/v1/files/upload")
                .header("Authorization", "Bearer $token")
                .post(body.build())
                .build()
            httpClient.newCall(request).execute().use { response ->
                val raw = response.body.string()
                check(response.isSuccessful) { "HTTP ${response.code}: ${raw.take(120)}" }
                json.decodeFromString(CloudArtifactDto.serializer(), raw)
            }
        }
    }

    suspend fun downloadArtifact(
        baseUrl: String,
        token: String,
        artifactId: String,
        dest: File,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/v1/files/$artifactId")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "HTTP ${response.code}" }
                val bytes = response.body.bytes()
                dest.parentFile?.mkdirs()
                dest.writeBytes(bytes)
                dest
            }
        }
    }

    suspend fun getProjectWorkspace(
        baseUrl: String,
        token: String,
        projectId: String,
    ): Result<CloudProjectWorkspaceDto> = withContext(Dispatchers.IO) {
        getJson(
            "${baseUrl.trimEnd('/')}/v1/projects/$projectId/workspace",
            token,
            CloudProjectWorkspaceDto.serializer(),
        )
    }

    private inline fun <reified T> getJson(url: String, token: String?, serializer: kotlinx.serialization.KSerializer<T>): Result<T> =
        runCatching {
            val builder = Request.Builder().url(url).get()
            if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
            httpClient.newCall(builder.build()).execute().use { response ->
                val raw = response.body.string()
                check(response.isSuccessful) { "HTTP ${response.code}: ${raw.take(120)}" }
                json.decodeFromString(serializer, raw)
            }
        }

    private inline fun <reified Req, reified Res> postJson(
        url: String,
        token: String,
        body: Req,
        serializer: kotlinx.serialization.KSerializer<Res>,
        bodySerializer: kotlinx.serialization.KSerializer<Req> = kotlinx.serialization.serializer(),
    ): Result<Res> = runCatching {
        val encoded = json.encodeToString(bodySerializer, body)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .post(encoded.toRequestBody(JSON_MEDIA))
            .build()
        httpClient.newCall(request).execute().use { response ->
            val raw = response.body.string()
            check(response.isSuccessful) { "HTTP ${response.code}: ${raw.take(120)}" }
            json.decodeFromString(serializer, raw)
        }
    }

    private inline fun <reified T> getJsonList(
        url: String,
        token: String,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): Result<List<T>> = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            val raw = response.body.string()
            check(response.isSuccessful) { "HTTP ${response.code}" }
            json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(serializer), raw)
        }
    }

    private fun providerLabel(id: String): String = when (id) {
        "chatgpt" -> "ChatGPT"
        "gemini" -> "Gemini"
        "deepseek" -> "DeepSeek"
        "auto" -> "Hassan Auto"
        else -> id
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
