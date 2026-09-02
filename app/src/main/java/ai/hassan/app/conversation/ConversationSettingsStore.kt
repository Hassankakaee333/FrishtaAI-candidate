package ai.hassan.app.conversation

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConversationSettings(
    val cloudBaseUrl: String = "",
    val accessToken: String = "",
    val chatProvider: String = "auto",
    val humanGatedFallbackEnabled: Boolean = true,
)

class ConversationSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<ConversationSettings> = _settings.asStateFlow()

    fun read(): ConversationSettings = ConversationSettings(
        cloudBaseUrl = prefs.getString(KEY_CLOUD_URL, "").orEmpty().trim(),
        accessToken = prefs.getString(KEY_TOKEN, "").orEmpty().trim(),
        chatProvider = prefs.getString(KEY_PROVIDER, "auto") ?: "auto",
        humanGatedFallbackEnabled = prefs.getBoolean(KEY_HUMAN_FALLBACK, true),
    )

    fun update(transform: (ConversationSettings) -> ConversationSettings) {
        val next = transform(read())
        prefs.edit()
            .putString(KEY_CLOUD_URL, next.cloudBaseUrl)
            .putString(KEY_TOKEN, next.accessToken)
            .putString(KEY_PROVIDER, next.chatProvider)
            .putBoolean(KEY_HUMAN_FALLBACK, next.humanGatedFallbackEnabled)
            .apply()
        _settings.value = next
    }

    fun isCloudConfigured(): Boolean {
        val s = read()
        return s.cloudBaseUrl.isNotBlank() && s.accessToken.isNotBlank()
    }

    companion object {
        private const val PREFS = "hassan_conversation_settings"
        private const val KEY_CLOUD_URL = "cloud_base_url"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_PROVIDER = "chat_provider"
        private const val KEY_HUMAN_FALLBACK = "human_gated_fallback"
        val CHAT_PROVIDER_IDS = setOf("auto", "chatgpt", "gemini", "claude", "deepseek")
    }
}
