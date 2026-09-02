package ai.hassan.app.conversation

import ai.hassan.app.cloud.HassanCloudApi

/**
 * Hassan Auto conversation: uses Hassan Cloud when configured.
 * No API keys in APK — credentials live in user settings / server.
 */
class HassanConversationProvider(
    private val settingsStore: ConversationSettingsStore,
    private val cloudApi: HassanCloudApi,
) : ConversationProvider {
    private val fallback = LocalHassanConversationProvider {
        settingsStore.read().chatProvider
    }

    override val isConfigured: Boolean
        get() = true

    override suspend fun sendMessage(
        history: List<ConversationMessage>,
        userMessage: String,
    ): ConversationResult {
        val settings = settingsStore.read()
        if (!settingsStore.isCloudConfigured()) {
            return fallback.sendMessage(history, userMessage)
        }
        val provider = settings.chatProvider.ifBlank { "auto" }
        return when (
            val result = cloudApi.chat(
                baseUrl = settings.cloudBaseUrl,
                accessToken = settings.accessToken,
                provider = provider,
                messages = history,
            )
        ) {
            is ConversationResult.Success -> result
            is ConversationResult.NotConfigured -> fallback.sendMessage(history, userMessage)
            is ConversationResult.Error -> {
                val local = fallback.sendMessage(history, userMessage)
                if (local is ConversationResult.Success) {
                    ConversationResult.Success(
                        answer = local.answer + "\n\n(ملاحظة: تعذر الرد من السحابة الآن — ${result.message.take(80)})",
                        providerId = local.providerId ?: LocalHassanChat.normalizeProvider(provider),
                    )
                } else {
                    result
                }
            }
        }
    }
}
