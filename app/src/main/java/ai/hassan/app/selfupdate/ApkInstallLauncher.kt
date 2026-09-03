package ai.hassan.app.selfupdate

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

object ApkInstallLauncher {
    fun createInstallIntent(context: Context, apkPath: String): Intent {
        val apkFile = File(apkPath)
        check(apkFile.exists() && apkFile.length() > 0L) { "ملف APK غير موجود أو فارغ" }
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                // Pre-N uses file://; keep NEW_TASK only.
            }
        }
        // Samsung / Android 14+ often need an explicit grant to the resolver package.
        val targets = context.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        for (resolve in targets) {
            context.grantUriPermission(
                resolve.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        return intent
    }
}
