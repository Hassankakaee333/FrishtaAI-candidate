package ai.hassan.app.conversation

data class ConversationMessage(
    val role: String,
    val content: String,
)

sealed class ConversationResult {
    data class Success(
        val answer: String,
        val providerId: String? = null,
        val codexUsage: CodexUsageSnapshot? = null,
    ) : ConversationResult()
    data class NotConfigured(val message: String) : ConversationResult()
    data class Error(val message: String) : ConversationResult()
}

/**
 * Standalone conversational AI boundary for the Android app.
 * No desktop/PC dependency — implement with a real on-device or configured provider later.
 */
interface ConversationProvider {
    val isConfigured: Boolean

    suspend fun sendMessage(
        history: List<ConversationMessage>,
        userMessage: String,
    ): ConversationResult
}

/** Honest default until a real standalone provider is wired. */
class UnconfiguredConversationProvider : ConversationProvider {
    override val isConfigured: Boolean = false

    override suspend fun sendMessage(
        history: List<ConversationMessage>,
        userMessage: String,
    ): ConversationResult = ConversationResult.NotConfigured(
        message = "لم يتم إعداد مزود المحادثة بعد.",
    )
}

data class ConversationUiState(
    val providerConfigured: Boolean = false,
    val statusMessage: String = "لم يتم إعداد مزود المحادثة بعد.",
    val isSending: Boolean = false,
    val codexUsage: CodexUsageSnapshot? = null,
)
