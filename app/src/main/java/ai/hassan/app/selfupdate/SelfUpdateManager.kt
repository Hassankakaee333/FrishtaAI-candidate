package ai.hassan.app.selfupdate

import android.content.Context
import android.content.pm.PackageManager
import ai.hassan.app.BuildConfig
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class SelfUpdateManager(
    private val context: Context,
    private val httpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val backupDir = File(context.filesDir, "apk-backups").apply { mkdirs() }
    private val stagingDir = File(context.filesDir, "apk-staging").apply { mkdirs() }
    private val backupApk = File(backupDir, "hassan-previous.apk")
    private val backupMeta = File(backupDir, "hassan-previous.json")
    private val stagedApk = File(stagingDir, "hassan-update.apk")

    private val _events = MutableSharedFlow<SelfUpdateEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<SelfUpdateEvent> = _events.asSharedFlow()

    suspend fun notifyUser(message: String) {
        _events.emit(SelfUpdateEvent.Notify(message))
    }

    fun hasBackup(): Boolean = backupApk.exists() && backupApk.length() > 0L

    fun hasStagedUpdate(): Boolean = stagedApk.exists() && stagedApk.length() > 0L

    fun backupMetadata(): ApkBackupMetadata? {
        if (!backupMeta.exists()) return null
        return runCatching {
            json.decodeFromString<ApkBackupMetadata>(backupMeta.readText())
        }.getOrNull()
    }

    suspend fun backupCurrentApk(): SelfUpdateResult = withContext(Dispatchers.IO) {
        runCatching {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
            val sourceApk = File(
                context.applicationInfo.sourceDir,
            )
            check(sourceApk.exists()) { "تعذر العثور على APK الحالي" }
            sourceApk.copyTo(backupApk, overwrite = true)
            val metadata = ApkBackupMetadata(
                versionCode = packageInfo.longVersionCode,
                versionName = packageInfo.versionName.orEmpty(),
                packageName = context.packageName,
                backedUpAt = System.currentTimeMillis(),
                sourcePath = sourceApk.absolutePath,
                backupPath = backupApk.absolutePath,
                sha256 = sha256(backupApk),
            )
            backupMeta.writeText(json.encodeToString(metadata))
            SelfUpdateResult.BackupCreated(metadata)
        }.getOrElse { SelfUpdateResult.Error(it.message ?: "فشل النسخ الاحتياطي") }
    }

    suspend fun stageApkFromPath(sourcePath: String): SelfUpdateResult = withContext(Dispatchers.IO) {
        runCatching {
            val source = File(sourcePath.removePrefix("file://"))
            check(source.exists()) { "ملف APK غير موجود" }
            source.copyTo(stagedApk, overwrite = true)
            SelfUpdateResult.UpdateReady(stagedApk.absolutePath, backupMetadata())
        }.getOrElse { SelfUpdateResult.Error(it.message ?: "تعذر تجهيز APK") }
    }

    suspend fun checkRemoteUpdate(): SelfUpdateResult = withContext(Dispatchers.IO) {
        val manifestUrl = BuildConfig.CANDIDATE_UPDATE_MANIFEST_URL
        if (manifestUrl.isBlank()) {
            return@withContext SelfUpdateResult.Message(
                "لا يوجد خادم تحديث مُعد بعد. يمكنك إرفاق ملف APK وطلب «ثبت التحديث».",
            )
        }
        runCatching {
            val request = Request.Builder()
                .url(manifestUrl)
                .header("User-Agent", "Hassan-SelfUpdate/${BuildConfig.VERSION_NAME}")
                .build()
            httpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "HTTP ${response.code}" }
                val body = response.body.string()
                val manifest = json.decodeFromString<RemoteUpdateManifest>(body)
                val current = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                ).longVersionCode
                if (manifest.versionCode <= current) {
                    SelfUpdateResult.Message("أنت على أحدث نسخة معروفة (${BuildConfig.VERSION_NAME}).")
                } else {
                    downloadRemoteApk(manifest)
                }
            }
        }.getOrElse { SelfUpdateResult.Error(it.message ?: "فشل التحقق من التحديث") }
    }

    private suspend fun downloadRemoteApk(manifest: RemoteUpdateManifest): SelfUpdateResult {
        val request = Request.Builder()
            .url(manifest.apkUrl)
            .header("User-Agent", "Hassan-SelfUpdate/${BuildConfig.VERSION_NAME}")
            .build()
        httpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "تعذر تنزيل APK: HTTP ${response.code}" }
            response.body.byteStream().use { input ->
                stagedApk.outputStream().use { output -> input.copyTo(output) }
            }
        }
        manifest.sha256?.let { expected ->
            val actual = sha256(stagedApk)
            check(actual.equals(expected, ignoreCase = true)) { "تحقق SHA-256 فشل" }
        }
        return SelfUpdateResult.UpdateReady(stagedApk.absolutePath, backupMetadata())
    }

    suspend fun requestInstallStagedApk(): SelfUpdateResult {
        if (!stagedApk.exists()) {
            return SelfUpdateResult.Error("لا يوجد تحديث جاهز للتثبيت.")
        }
        _events.emit(SelfUpdateEvent.RequestInstall(stagedApk.absolutePath))
        return SelfUpdateResult.Message("افتح شاشة التثبيت ووافق على التحديث.")
    }

    /**
     * Backup current APK, stage a downloaded file, then prompt install.
     */
    suspend fun installUpdateFromFile(sourcePath: String, backupFirst: Boolean = true): List<SelfUpdateResult> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<SelfUpdateResult>()
            if (backupFirst && !hasBackup()) {
                results += backupCurrentApk()
            } else if (backupFirst) {
                // Refresh backup so rollback matches the version being replaced.
                results += backupCurrentApk()
            }
            when (val staged = stageApkFromPath(sourcePath)) {
                is SelfUpdateResult.UpdateReady -> {
                    results += staged
                    results += requestInstallStagedApk()
                }
                is SelfUpdateResult.Error -> results += staged
                else -> results += SelfUpdateResult.Error("تعذر تجهيز ملف التحديث.")
            }
            results
        }

    suspend fun requestRollback(): SelfUpdateResult {
        if (!hasBackup()) {
            return SelfUpdateResult.Error("لا توجد نسخة APK احتياطية محفوظة.")
        }
        _events.emit(SelfUpdateEvent.RequestInstall(backupApk.absolutePath))
        return SelfUpdateResult.Message("سيتم تثبيت النسخة الاحتياطية بعد موافقتك.")
    }

    suspend fun handleSelfImprovement(
        action: SelfImprovementAction,
        attachedApkPath: String? = null,
    ): List<SelfUpdateResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SelfUpdateResult>()
        when (action) {
            SelfImprovementAction.APPLY_UPDATE -> {
                val backup = backupCurrentApk()
                results += backup
                when (val staged = attachedApkPath?.let { stageApkFromPath(it) }) {
                    is SelfUpdateResult.UpdateReady -> {
                        results += staged
                        results += requestInstallStagedApk()
                    }
                    is SelfUpdateResult.Error -> results += staged
                    null -> {
                        val remote = checkRemoteUpdate()
                        results += remote
                        if (remote is SelfUpdateResult.UpdateReady) {
                            results += requestInstallStagedApk()
                        }
                    }
                    else -> Unit
                }
            }
            SelfImprovementAction.REQUEST_SELF_IMPROVE -> {
                results += SelfUpdateResult.Message(
                    "طلب التعديل الذاتي يُعالج من المحادثة عبر السحابة (نسخ احتياطي → بناء → تثبيت).",
                )
            }
            SelfImprovementAction.CHECK_UPDATE -> {
                results += checkRemoteUpdate()
            }
            SelfImprovementAction.ROLLBACK -> {
                results += requestRollback()
            }
        }
        results
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = input.read(buffer)
            while (read >= 0) {
                if (read > 0) digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
