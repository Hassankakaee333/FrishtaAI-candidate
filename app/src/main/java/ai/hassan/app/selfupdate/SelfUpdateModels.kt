package ai.hassan.app.selfupdate

import kotlinx.serialization.Serializable

@Serializable
data class ApkBackupMetadata(
    val versionCode: Long,
    val versionName: String,
    val packageName: String,
    val backedUpAt: Long,
    val sourcePath: String,
    val backupPath: String,
    val sha256: String,
)

@Serializable
data class RemoteUpdateManifest(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val sha256: String? = null,
    val releaseNotes: String? = null,
)

sealed class SelfUpdateResult {
    data class BackupCreated(val metadata: ApkBackupMetadata) : SelfUpdateResult()
    data class UpdateReady(val apkPath: String, val metadata: ApkBackupMetadata?) : SelfUpdateResult()
    data class Message(val text: String) : SelfUpdateResult()
    data class Error(val text: String) : SelfUpdateResult()
}

sealed class SelfUpdateEvent {
    data class RequestInstall(val apkPath: String) : SelfUpdateEvent()
    data class Notify(val message: String) : SelfUpdateEvent()
}
