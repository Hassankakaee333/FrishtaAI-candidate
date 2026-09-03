package ai.hassan.app.phoneagent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import ai.hassan.app.HassanApplication
import java.io.File
import java.util.ArrayDeque
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * System-bound execution layer for Hassan Phone Agent.
 * It deliberately never exposes password text and refuses to type into password fields.
 */
class HassanAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var currentPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val app = application as HassanApplication
        serviceScope.launch {
            PhoneAgentCloudBridge(
                context = this@HassanAccessibilityService,
                settingsStore = app.container.conversationSettingsStore,
                cloudApi = app.container.hassanCloudApi,
                service = this@HassanAccessibilityService,
            ).runLoop()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.packageName?.toString()?.takeIf { it.isNotBlank() }?.let { currentPackage = it }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        serviceScope.cancel()
        super.onDestroy()
    }

    fun activePackage(): String? = currentPackage ?: rootInActiveWindow?.packageName?.toString()

    internal suspend fun execute(command: PhoneAgentCommand): PhoneExecutionResult =
        withContext(Dispatchers.Main.immediate) {
            when (command.action) {
                PhoneAgentActions.PING -> PhoneExecutionResult(true, "الهاتف متصل وجاهز")
                PhoneAgentActions.UI_TREE -> PhoneExecutionResult(true, "تمت قراءة الواجهة", uiTree = dumpUiTree())
                PhoneAgentActions.OPEN_APP -> openApp(command.packageName)
                PhoneAgentActions.HOME -> global(GLOBAL_ACTION_HOME, "تم فتح الشاشة الرئيسية")
                PhoneAgentActions.BACK -> global(GLOBAL_ACTION_BACK, "تم الرجوع")
                PhoneAgentActions.RECENTS -> global(GLOBAL_ACTION_RECENTS, "تم فتح التطبيقات الأخيرة")
                PhoneAgentActions.NOTIFICATIONS -> global(GLOBAL_ACTION_NOTIFICATIONS, "تم فتح الإشعارات")
                PhoneAgentActions.QUICK_SETTINGS -> global(GLOBAL_ACTION_QUICK_SETTINGS, "تم فتح الإعدادات السريعة")
                PhoneAgentActions.CLICK_TEXT -> clickText(command.targetText)
                PhoneAgentActions.SET_TEXT -> setText(command.targetText, command.text)
                PhoneAgentActions.TAP -> tap(command.x, command.y, command.durationMs)
                PhoneAgentActions.SWIPE -> swipe(
                    command.x,
                    command.y,
                    command.endX,
                    command.endY,
                    command.durationMs,
                )
                PhoneAgentActions.SCROLL_FORWARD -> scroll(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                PhoneAgentActions.SCROLL_BACKWARD -> scroll(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                PhoneAgentActions.SCREENSHOT -> screenshot(command.id)
                else -> PhoneExecutionResult(false, "أمر غير مدعوم: ${command.action}")
            }
        }

    private fun global(action: Int, successMessage: String): PhoneExecutionResult {
        val ok = performGlobalAction(action)
        return PhoneExecutionResult(ok, if (ok) successMessage else "تعذر تنفيذ أمر النظام")
    }

    private fun openApp(packageName: String?): PhoneExecutionResult {
        val target = packageName.orEmpty().trim()
        if (target.isBlank()) return PhoneExecutionResult(false, "اسم الحزمة مطلوب")
        val intent = packageManager.getLaunchIntentForPackage(target)
            ?: return PhoneExecutionResult(false, "لم أجد تطبيقًا بالحزمة $target")
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            startActivity(intent)
            PhoneExecutionResult(true, "تم فتح $target")
        }.getOrElse { PhoneExecutionResult(false, "تعذر فتح التطبيق: ${it.message}") }
    }

    private fun clickText(targetText: String?): PhoneExecutionResult {
        val needle = targetText.orEmpty().trim()
        if (needle.isBlank()) return PhoneExecutionResult(false, "النص المستهدف مطلوب")
        val node = findNode { info ->
            val text = info.text?.toString().orEmpty()
            val desc = info.contentDescription?.toString().orEmpty()
            text.contains(needle, ignoreCase = true) || desc.contains(needle, ignoreCase = true)
        } ?: return PhoneExecutionResult(false, "لم أجد: $needle", uiTree = dumpUiTree())
        var clickable: AccessibilityNodeInfo? = node
        while (clickable != null && !clickable.isClickable) clickable = clickable.parent
        val ok = clickable?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
        return PhoneExecutionResult(ok, if (ok) "تم الضغط على $needle" else "العنصر موجود لكنه غير قابل للضغط")
    }

    private fun setText(targetText: String?, value: String?): PhoneExecutionResult {
        val newValue = value ?: return PhoneExecutionResult(false, "النص المراد كتابته مطلوب")
        val needle = targetText.orEmpty().trim()
        val candidates = collectNodes().filter { it.isEditable && !it.isPassword }
        val node = when {
            needle.isNotBlank() -> candidates.firstOrNull { info ->
                info.text?.toString().orEmpty().contains(needle, true) ||
                    info.contentDescription?.toString().orEmpty().contains(needle, true) ||
                    info.viewIdResourceName.orEmpty().contains(needle, true)
            }
            else -> candidates.firstOrNull { it.isFocused } ?: candidates.firstOrNull()
        } ?: return PhoneExecutionResult(false, "لم أجد حقل كتابة آمن", uiTree = dumpUiTree())
        if (node.isPassword) return PhoneExecutionResult(false, "رفضت الكتابة في حقل كلمة مرور")
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newValue)
        }
        val ok = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        return PhoneExecutionResult(ok, if (ok) "تم إدخال النص" else "تعذر إدخال النص")
    }

    private suspend fun tap(x: Float?, y: Float?, durationMs: Long): PhoneExecutionResult {
        if (x == null || y == null) return PhoneExecutionResult(false, "إحداثيات الضغط مطلوبة")
        val path = Path().apply { moveTo(x, y) }
        val ok = dispatchGestureAwait(path, durationMs.coerceIn(80, 1_500))
        return PhoneExecutionResult(ok, if (ok) "تم الضغط عند $x,$y" else "فشل الضغط")
    }

    private suspend fun swipe(
        x: Float?,
        y: Float?,
        endX: Float?,
        endY: Float?,
        durationMs: Long,
    ): PhoneExecutionResult {
        if (x == null || y == null || endX == null || endY == null) {
            return PhoneExecutionResult(false, "إحداثيات السحب غير مكتملة")
        }
        val path = Path().apply { moveTo(x, y); lineTo(endX, endY) }
        val ok = dispatchGestureAwait(path, durationMs.coerceIn(120, 5_000))
        return PhoneExecutionResult(ok, if (ok) "تم السحب" else "فشل السحب")
    }

    private suspend fun dispatchGestureAwait(path: Path, durationMs: Long): Boolean =
        suspendCancellableCoroutine { continuation ->
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
                .build()
            val accepted = dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                },
                null,
            )
            if (!accepted && continuation.isActive) continuation.resume(false)
        }

    private fun scroll(action: Int): PhoneExecutionResult {
        val node = findNode { it.isScrollable } ?: rootInActiveWindow
            ?: return PhoneExecutionResult(false, "لا توجد واجهة قابلة للتمرير")
        val ok = node.performAction(action)
        return PhoneExecutionResult(ok, if (ok) "تم التمرير" else "تعذر التمرير")
    }

    private suspend fun screenshot(commandId: String): PhoneExecutionResult =
        suspendCancellableCoroutine { continuation ->
            val outputDir = File(cacheDir, "phone-agent").apply { mkdirs() }
            val file = File(outputDir, "${safeId(commandId)}-${System.currentTimeMillis()}.png")
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        val buffer = screenshot.hardwareBuffer
                        val hardware = Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace)
                        val bitmap = hardware?.copy(Bitmap.Config.ARGB_8888, false)
                        buffer.close()
                        val ok = bitmap != null && runCatching {
                            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        }.getOrDefault(false)
                        bitmap?.recycle()
                        if (continuation.isActive) {
                            continuation.resume(
                                if (ok) PhoneExecutionResult(true, "تم التقاط الشاشة", screenshotFilePath = file.absolutePath)
                                else PhoneExecutionResult(false, "تعذر حفظ لقطة الشاشة"),
                            )
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        if (continuation.isActive) continuation.resume(
                            PhoneExecutionResult(false, "تعذر التقاط الشاشة (رمز $errorCode)"),
                        )
                    }
                },
            )
        }

    private fun dumpUiTree(): String {
        val nodes = collectNodes(limit = 280)
        if (nodes.isEmpty()) return "<no-accessible-ui>"
        return buildString {
            nodes.forEachIndexed { index, node ->
                val bounds = Rect().also(node::getBoundsInScreen)
                val text = if (node.isPassword) "<password>" else node.text?.toString().orEmpty().take(120)
                val desc = if (node.isPassword) "<password>" else node.contentDescription?.toString().orEmpty().take(120)
                append(index).append('|')
                append(node.packageName?.toString().orEmpty()).append('|')
                append(node.className?.toString().orEmpty()).append('|')
                append("text=").append(text.replace('\n', ' ')).append('|')
                append("desc=").append(desc.replace('\n', ' ')).append('|')
                append("id=").append(node.viewIdResourceName.orEmpty()).append('|')
                append("bounds=").append(bounds.flattenToString()).append('|')
                append("clickable=").append(node.isClickable).append('|')
                append("editable=").append(node.isEditable).append('|')
                append("password=").append(node.isPassword).append('\n')
            }
        }.take(60_000)
    }

    private fun findNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? =
        collectNodes(limit = 400).firstOrNull(predicate)

    private fun collectNodes(limit: Int = 400): List<AccessibilityNodeInfo> {
        val roots = windows.mapNotNull { it.root }.ifEmpty { listOfNotNull(rootInActiveWindow) }
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        roots.forEach(queue::add)
        val result = ArrayList<AccessibilityNodeInfo>(minOf(limit, 128))
        while (queue.isNotEmpty() && result.size < limit) {
            val node = queue.removeFirst()
            result += node
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::addLast)
        }
        return result
    }

    private fun safeId(id: String): String = id.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80)

    companion object {
        @Volatile var instance: HassanAccessibilityService? = null
            private set
    }
}
