package ai.hassan.app.diagnostics

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.biometric.BiometricManager
import ai.hassan.app.BuildConfig
import ai.hassan.app.identity.DeviceIdentityManager
import java.text.DateFormat
import java.util.Date

data class DiagnosticItem(
    val label: String,
    val value: String,
)

data class DiagnosticsReport(
    val items: List<DiagnosticItem>,
) {
    fun asPlainText(): String = buildString {
        appendLine("Hassan AI — Diagnostics")
        items.forEach { appendLine("${it.label}: ${it.value}") }
    }
}

class DiagnosticsCollector(
    private val activity: Activity,
    private val identityManager: DeviceIdentityManager,
) {
    fun collect(): DiagnosticsReport {
        val context = activity.applicationContext
        val memoryInfo = ActivityManager.MemoryInfo().also {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it)
        }
        val bounds = activity.windowManager.currentWindowMetrics.bounds
        val density = activity.resources.displayMetrics.density
        val identity = identityManager.ensureIdentity()
        val installer = runCatching {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        }.getOrNull() ?: "ADB / غير معروف"

        return DiagnosticsReport(
            listOf(
                DiagnosticItem("وقت التقرير", DateFormat.getDateTimeInstance().format(Date())),
                DiagnosticItem("الجهاز", "${Build.MANUFACTURER} ${Build.MODEL}"),
                DiagnosticItem("Android", "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"),
                DiagnosticItem("ABI", Build.SUPPORTED_ABIS.joinToString()),
                DiagnosticItem(
                    "الذاكرة",
                    "${bytesToGiB(memoryInfo.availMem)} GB متاحة من ${bytesToGiB(memoryInfo.totalMem)} GB",
                ),
                DiagnosticItem(
                    "النافذة",
                    "${bounds.width()}×${bounds.height()} px · ${(bounds.width() / density).toInt()}×${(bounds.height() / density).toInt()} dp",
                ),
                DiagnosticItem(
                    "وضع النافذة / DeX",
                    if (activity.isInMultiWindowMode) "نافذة متعددة / DeX محتمل" else desktopModeLabel(),
                ),
                DiagnosticItem("الاتصال", networkLabel(context)),
                DiagnosticItem("التحقق البيومتري", biometricLabel(context)),
                DiagnosticItem(
                    "Android Keystore",
                    if (identity.insideSecureHardware) "Hardware-backed" else "Software-backed",
                ),
                DiagnosticItem(
                    "StrongBox",
                    if (identity.strongBoxBacked) "المفتاح الحالي داخل StrongBox" else "Fallback آمن مستخدم",
                ),
                DiagnosticItem("بصمة المفتاح العام", identity.publicKeyFingerprint),
                DiagnosticItem("قناة البناء", BuildConfig.CHANNEL),
                DiagnosticItem("الإصدار", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"),
                DiagnosticItem("مصدر التثبيت", installer),
                DiagnosticItem("FCM", "غير مكوّن في Milestone 2"),
                DiagnosticItem("Backend", "Control Plane المحلي مختبر؛ لا توجد Cloudflare credentials داخل APK"),
            ),
        )
    }

    private fun desktopModeLabel(): String {
        val type = activity.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
        return if (type == Configuration.UI_MODE_TYPE_DESK) "Desktop mode" else "هاتف بملء الشاشة"
    }

    private fun networkLabel(context: Context): String {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return "غير متصل"
        val transport = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi‑Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "بيانات هاتف"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "شبكة أخرى"
        }
        return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            "$transport · إنترنت متحقق"
        } else {
            "$transport · دون تحقق إنترنت"
        }
    }

    private fun biometricLabel(context: Context): String {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return when (BiometricManager.from(context).canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "جاهز (قوي أو قفل الجهاز)"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "لا توجد بصمة/قفل مسجّل"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "العتاد غير متوفر"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "العتاد غير متاح مؤقتًا"
            else -> "غير متاح"
        }
    }

    private fun bytesToGiB(value: Long): String = "%.1f".format(value / 1024.0 / 1024.0 / 1024.0)
}
