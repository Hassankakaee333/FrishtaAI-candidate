package ai.hassan.app.phoneagent

import kotlinx.serialization.Serializable

@Serializable
data class PhoneAgentCommand(
    val id: String,
    val action: String,
    val packageName: String? = null,
    val targetText: String? = null,
    val text: String? = null,
    val x: Float? = null,
    val y: Float? = null,
    val endX: Float? = null,
    val endY: Float? = null,
    val durationMs: Long = 350,
    val requiresConfirmation: Boolean = false,
    val expiresAtEpochMs: Long = 0,
)

@Serializable
data class PhoneAgentResult(
    val id: String,
    val status: String,
    val message: String,
    val activePackage: String? = null,
    val uiTree: String? = null,
    val screenshotArtifactId: String? = null,
    val completedAtEpochMs: Long = System.currentTimeMillis(),
)

internal data class PhoneExecutionResult(
    val ok: Boolean,
    val message: String,
    val uiTree: String? = null,
    val screenshotFilePath: String? = null,
)

object PhoneAgentActions {
    const val PING = "PING"
    const val UI_TREE = "UI_TREE"
    const val OPEN_APP = "OPEN_APP"
    const val HOME = "HOME"
    const val BACK = "BACK"
    const val RECENTS = "RECENTS"
    const val NOTIFICATIONS = "NOTIFICATIONS"
    const val QUICK_SETTINGS = "QUICK_SETTINGS"
    const val CLICK_TEXT = "CLICK_TEXT"
    const val SET_TEXT = "SET_TEXT"
    const val TAP = "TAP"
    const val SWIPE = "SWIPE"
    const val SCROLL_FORWARD = "SCROLL_FORWARD"
    const val SCROLL_BACKWARD = "SCROLL_BACKWARD"
    const val SCREENSHOT = "SCREENSHOT"

    val supported = setOf(
        PING, UI_TREE, OPEN_APP, HOME, BACK, RECENTS,
        NOTIFICATIONS, QUICK_SETTINGS, CLICK_TEXT, SET_TEXT,
        TAP, SWIPE, SCROLL_FORWARD, SCROLL_BACKWARD, SCREENSHOT,
    )
}
