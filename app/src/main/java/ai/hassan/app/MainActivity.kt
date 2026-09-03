package ai.hassan.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import ai.hassan.app.diagnostics.DiagnosticsCollector
import ai.hassan.app.providers.HumanBridgeLauncher
import ai.hassan.app.selfupdate.ApkInstallLauncher
import ai.hassan.app.selfupdate.SelfUpdateEvent
import ai.hassan.app.ui.HassanApp
import ai.hassan.app.ui.MainViewModel
import ai.hassan.app.ui.theme.HassanTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val container
        get() = (application as HassanApplication).container

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(container.repository)
    }

    private var pendingBiometricDecisionId: String? = null

    private val bridgeLauncher by lazy { HumanBridgeLauncher(this) }

    private val biometricPrompt by lazy {
        BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    pendingBiometricDecisionId?.let(viewModel::approveDecision)
                    pendingBiometricDecisionId = null
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    pendingBiometricDecisionId = null
                    Toast.makeText(this@MainActivity, errString, Toast.LENGTH_SHORT).show()
                }
            },
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncCloudJobs()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Let Compose imePadding own the keyboard inset. ADJUST_RESIZE + imePadding
        // double-applies on Samsung and leaves a huge gap above the keyboard.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        handleShareIntent(intent)
        lifecycleScope.launch {
            container.repository.selfUpdateEvents.collect { event ->
                when (event) {
                    is SelfUpdateEvent.RequestInstall -> launchApkInstall(event.apkPath)
                    is SelfUpdateEvent.Notify -> Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
        setContent {
            HassanTheme {
                HassanApp(
                    viewModel = viewModel,
                    diagnosticsCollector = DiagnosticsCollector(
                        this@MainActivity,
                        container.identityManager,
                    ),
                    onApproveWithBiometric = ::authenticateDecision,
                    onLaunchBridge = ::launchHumanBridge,
                    onCopyDiagnostics = ::copyDiagnostics,
                    onShareDiagnostics = ::shareDiagnostics,
                )
            }
        }
    }

    private fun launchApkInstall(apkPath: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !packageManager.canRequestPackageInstalls()
        ) {
            Toast.makeText(
                this,
                "اسمح بتثبيت التطبيقات من Frishta ثم اضغط تثبيت مرة أخرى",
                Toast.LENGTH_LONG,
            ).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                },
            )
            return
        }
        runCatching {
            val intent = ApkInstallLauncher.createInstallIntent(this, apkPath)
            startActivity(intent)
            Toast.makeText(this, "افتح شاشة التثبيت ووافق على التحديث", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "تعذر فتح مثبت APK: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun launchHumanBridge(providerId: String, taskPackText: String) {
        startActivity(bridgeLauncher.createIntent(providerId, taskPackText))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun authenticateDecision(decisionId: String) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) !=
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            Toast.makeText(this, "التحقق البيومتري غير جاهز على الجهاز", Toast.LENGTH_LONG).show()
            return
        }
        pendingBiometricDecisionId = decisionId
        biometricPrompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("اعتماد قرار Hassan")
                .setSubtitle("تحقق من هويتك قبل توقيع القرار بمفتاح الجهاز")
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }

    private fun handleShareIntent(source: Intent?) {
        if (source?.action != Intent.ACTION_SEND) return
        val text = source.getStringExtra(Intent.EXTRA_TEXT)
        val image = source.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.toString()
        viewModel.ingestShare(text, image)
        source.action = null
    }

    private fun copyDiagnostics(report: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Hassan AI Diagnostics", report))
        Toast.makeText(this, "تم نسخ التقرير", Toast.LENGTH_SHORT).show()
    }

    private fun shareDiagnostics(report: String) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Hassan AI Diagnostics")
                    putExtra(Intent.EXTRA_TEXT, report)
                },
                "مشاركة تقرير Diagnostics",
            ),
        )
    }
}
