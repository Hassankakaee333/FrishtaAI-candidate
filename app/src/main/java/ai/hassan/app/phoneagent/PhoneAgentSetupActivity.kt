package ai.hassan.app.phoneagent

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import ai.hassan.app.HassanApplication

/** One-time setup screen. Android itself must grant Accessibility once. */
class PhoneAgentSetupActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var enableButton: Button
    private val prefs by lazy { PhoneAgentPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Hassan Phone Agent"
        setContentView(buildContent())
    }

    override fun onResume() {
        super.onResume()
        if (PhoneAgentPreferences.isAccessibilityEnabled(this)) {
            prefs.enableAfterAccessibilityGrant()
        }
        refreshStatus()
    }

    private fun buildContent(): View {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, dp(10), 0, dp(16))
        }
        enableButton = Button(this).apply {
            setOnClickListener {
                if (PhoneAgentPreferences.isAccessibilityEnabled(this@PhoneAgentSetupActivity)) {
                    prefs.setEnabled(true)
                    refreshStatus()
                } else {
                    prefs.markSetupPrompted()
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
        }
        val stopButton = Button(this).apply {
            text = "إيقاف التحكم السحابي"
            setOnClickListener {
                prefs.setEnabled(false)
                refreshStatus()
            }
        }
        val closeButton = Button(this).apply {
            text = "تم — العودة إلى Frishta"
            setOnClickListener { finish() }
        }

        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(24), dp(28), dp(24), dp(28))
            addView(TextView(this@PhoneAgentSetupActivity).apply {
                text = "Hassan Phone Agent"
                textSize = 26f
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(this@PhoneAgentSetupActivity).apply {
                text = "تحكم سحابي عام بالهاتف بدون كمبيوتر أو ADB. بعد تفعيل خدمة إمكانية الوصول مرة واحدة، يستطيع الوكيل فتح التطبيقات والتنقل والضغط والكتابة والتمرير وقراءة الواجهة والتقاط الشاشة عبر Hassan Cloud."
                textSize = 16f
                setPadding(0, dp(14), 0, dp(8))
            })
            addView(TextView(this@PhoneAgentSetupActivity).apply {
                text = "الحماية: كلمات المرور لا تُقرأ ولا تُكتب، والأوامر المصنفة حساسة تتوقف حتى موافقة محلية. زر الإيقاف يبقى نافذًا حتى تشغّل الوكيل بنفسك من جديد."
                textSize = 14f
                setPadding(0, 0, 0, dp(8))
            })
            addView(status)
            addView(enableButton, LinearLayout.LayoutParams(-1, -2))
            addView(stopButton, LinearLayout.LayoutParams(-1, -2))
            addView(closeButton, LinearLayout.LayoutParams(-1, -2))
        }
        return ScrollView(this).apply { addView(body) }
    }

    private fun refreshStatus() {
        val accessibility = PhoneAgentPreferences.isAccessibilityEnabled(this)
        val enabled = prefs.isEnabled()
        val cloudConfigured = (application as HassanApplication)
            .container.conversationSettingsStore.isCloudConfigured()
        status.text = buildString {
            append("إمكانية الوصول: ").append(if (accessibility) "مفعّلة ✅" else "غير مفعّلة")
            append("\nالوكيل السحابي: ").append(if (enabled && accessibility) "يعمل ✅" else "متوقف")
            append("\nHassan Cloud: ").append(if (cloudConfigured) "متصل ✅" else "يحتاج إعداد الرابط والرمز")
        }
        enableButton.text = when {
            !accessibility -> "تفعيل التحكم — خطوة واحدة"
            enabled -> "التحكم السحابي يعمل"
            else -> "تشغيل التحكم السحابي"
        }
        enableButton.isEnabled = !enabled || !accessibility
    }
}
