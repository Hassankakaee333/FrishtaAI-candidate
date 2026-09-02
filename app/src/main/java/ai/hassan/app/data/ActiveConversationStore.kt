package ai.hassan.app.data

import android.content.Context

/** Persists which conversation the user is viewing across app restarts. */
class ActiveConversationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(): String? = prefs.getString(KEY_ACTIVE_ID, null)

    fun write(conversationId: String) {
        prefs.edit().putString(KEY_ACTIVE_ID, conversationId).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_ACTIVE_ID).apply()
    }

    companion object {
        private const val PREFS_NAME = "hassan_conversation_prefs"
        private const val KEY_ACTIVE_ID = "active_conversation_id"
    }
}
