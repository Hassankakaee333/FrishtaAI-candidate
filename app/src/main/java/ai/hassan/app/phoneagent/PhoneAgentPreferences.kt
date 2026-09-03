package ai.hassan.app.phoneagent

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

class PhoneAgentPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)
    fun isUserStopped(): Boolean = prefs.getBoolean(KEY_USER_STOPPED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putBoolean(KEY_USER_STOPPED, !enabled)
            .apply()
    }

    fun enableAfterAccessibilityGrant() {
        if (!isUserStopped()) {
            prefs.edit().putBoolean(KEY_ENABLED, true).apply()
        }
    }

    fun wasSetupPrompted(): Boolean = prefs.getBoolean(KEY_SETUP_PROMPTED, false)

    fun markSetupPrompted() {
        prefs.edit().putBoolean(KEY_SETUP_PROMPTED, true).apply()
    }

    fun processedPaths(): MutableSet<String> =
        prefs.getStringSet(KEY_PROCESSED_PATHS, emptySet()).orEmpty().toMutableSet()

    fun markProcessed(path: String) {
        val next = processedPaths()
        next += path
        val stored = if (next.size > MAX_PROCESSED) {
            next.toList().takeLast(MAX_PROCESSED / 2).toSet()
        } else {
            next
        }
        prefs.edit().putStringSet(KEY_PROCESSED_PATHS, stored).apply()
    }

    companion object {
        private const val PREFS = "hassan_phone_agent"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_USER_STOPPED = "user_stopped"
        private const val KEY_SETUP_PROMPTED = "setup_prompted"
        private const val KEY_PROCESSED_PATHS = "processed_paths"
        private const val MAX_PROCESSED = 500

        fun isAccessibilityEnabled(context: Context): Boolean {
            val expected = ComponentName(context, HassanAccessibilityService::class.java).flattenToString()
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ).orEmpty()
            return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
        }
    }
}
