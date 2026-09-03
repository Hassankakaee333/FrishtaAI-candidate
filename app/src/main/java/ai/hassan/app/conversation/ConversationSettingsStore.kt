package ai.hassan.app.conversation

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConversationSettings(
    val cloudBaseUrl: String = "",
    val accessToken: String = "",
    val chatProvider: String = "auto",
    val speechLanguage: String = DEFAULT_SPEECH_LANGUAGE,
    val humanGatedFallbackEnabled: Boolean = true,
) {
    companion object {
        const val DEFAULT_SPEECH_LANGUAGE = "ar-IQ"
    }
}

class ConversationSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val secureTokenStore = SecureTokenStore(prefs)

    init {
        migrateLegacyPlaintextToken()
    }

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<ConversationSettings> = _settings.asStateFlow()

    fun read(): ConversationSettings = ConversationSettings(
        cloudBaseUrl = prefs.getString(KEY_CLOUD_URL, "").orEmpty().trim(),
        accessToken = secureTokenStore.read().trim(),
        chatProvider = prefs.getString(KEY_PROVIDER, "auto") ?: "auto",
        speechLanguage = prefs.getString(KEY_SPEECH_LANGUAGE, ConversationSettings.DEFAULT_SPEECH_LANGUAGE)
            ?: ConversationSettings.DEFAULT_SPEECH_LANGUAGE,
        humanGatedFallbackEnabled = prefs.getBoolean(KEY_HUMAN_FALLBACK, true),
    )

    fun update(transform: (ConversationSettings) -> ConversationSettings) {
        val next = transform(read())
        secureTokenStore.write(next.accessToken.trim())
        prefs.edit()
            .putString(KEY_CLOUD_URL, next.cloudBaseUrl)
            .putString(KEY_PROVIDER, next.chatProvider)
            .putString(KEY_SPEECH_LANGUAGE, next.speechLanguage)
            .putBoolean(KEY_HUMAN_FALLBACK, next.humanGatedFallbackEnabled)
            .remove(KEY_LEGACY_TOKEN)
            .apply()
        _settings.value = next.copy(accessToken = next.accessToken.trim())
    }

    fun isCloudConfigured(): Boolean {
        val s = read()
        return s.cloudBaseUrl.isNotBlank() && s.accessToken.isNotBlank()
    }

    private fun migrateLegacyPlaintextToken() {
        val legacy = prefs.getString(KEY_LEGACY_TOKEN, "").orEmpty().trim()
        if (legacy.isBlank()) return
        runCatching { secureTokenStore.write(legacy) }
            .onSuccess { prefs.edit().remove(KEY_LEGACY_TOKEN).apply() }
    }

    companion object {
        private const val PREFS = "hassan_conversation_settings"
        private const val KEY_CLOUD_URL = "cloud_base_url"
        private const val KEY_LEGACY_TOKEN = "access_token"
        private const val KEY_PROVIDER = "chat_provider"
        private const val KEY_SPEECH_LANGUAGE = "speech_language"
        private const val KEY_HUMAN_FALLBACK = "human_gated_fallback"
        val CHAT_PROVIDER_IDS = setOf("auto", "chatgpt", "gemini", "claude", "deepseek")
    }
}
