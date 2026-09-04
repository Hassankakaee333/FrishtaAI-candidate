package ai.hassan.app.conversation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
data class CloudChatMessageDto(
    val role: String,
    val content: String,
)

@Serializable
data class CloudChatRequest(
    val provider: String,
    val messages: List<CloudChatMessageDto>,
    val conversationId: String? = null,
)

@Serializable
data class CloudChatResponse(
    val answer: String,
    val provider: String? = null,
    val model: String? = null,
    val status: String? = null,
    val codex_usage: JsonElement? = null,
    val codexUsage: JsonElement? = null,
    val rate_limits: JsonElement? = null,
    val rateLimits: JsonElement? = null,
)

class CloudConversationClient(
    private val httpClient: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
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
                .header("Accept", "application/json")
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
                } else {
                    val label = parsed.provider?.let(::providerLabel)
                    val suffix = label?.let { "\n\n— $it" }.orEmpty()
                    ConversationResult.Success(
                        answer = parsed.answer.trim() + suffix,
                        codexUsage = CodexUsageParser.parse(
                            parsed.codex_usage
                                ?: parsed.codexUsage
                                ?: parsed.rate_limits
                                ?: parsed.rateLimits,
                        ),
                    )
                }
            }
        }.getOrElse {
            ConversationResult.Error(message = "تعذر الوصول إلى Hassan Cloud: ${it.message ?: "خطأ شبكة"}")
        }
    }

    private fun providerLabel(id: String): String = when (id) {
        "chatgpt" -> "ChatGPT / Codex"
        "gemini" -> "Gemini"
        "deepseek" -> "DeepSeek"
        "auto" -> "Hassan Auto"
        else -> id
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
