package ai.hassan.app.providers

import android.content.Context
import android.content.Intent

data class BridgeTarget(
    val providerId: String,
    val packageName: String,
    val displayName: String,
)

class HumanBridgeLauncher(private val context: Context) {
    fun createIntent(providerId: String, taskPackText: String): Intent {
        val target = targets.firstOrNull { it.providerId == providerId }
        val base = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Hassan AI TaskPack")
            putExtra(Intent.EXTRA_TEXT, taskPackText)
        }
        if (target != null && context.packageManager.getLaunchIntentForPackage(target.packageName) != null) {
            base.setPackage(target.packageName)
        }
        return Intent.createChooser(base, "إرسال TaskPack — أعد الرد إلى Hassan AI عبر المشاركة")
    }

    fun isInstalled(providerId: String): Boolean {
        val target = targets.firstOrNull { it.providerId == providerId } ?: return false
        return context.packageManager.getLaunchIntentForPackage(target.packageName) != null
    }

    companion object {
        val targets = listOf(
            BridgeTarget("chatgpt", "com.openai.chatgpt", "ChatGPT"),
            BridgeTarget("gemini", "com.google.android.apps.bard", "Gemini"),
            BridgeTarget("deepseek", "com.deepseek.chat", "DeepSeek"),
        )
    }
}
