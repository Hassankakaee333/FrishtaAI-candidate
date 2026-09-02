package ai.hassan.app.cloud

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

/** Verifies a real sample APK build through the public Worker and GitHub Actions. */
@RunWith(AndroidJUnit4::class)
class CloudAndroidBuildSmokeTest {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Test
    fun productionAndroidBuildCompletesWithDownloadableApk() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("hassan_conversation_settings", Context.MODE_PRIVATE)
        val baseUrl = prefs.getString("cloud_base_url", "").orEmpty().trim()
        val token = prefs.getString("access_token", "").orEmpty().trim()
        assertTrue("cloud_base_url not configured on device", baseUrl.startsWith("https://"))
        assertTrue("access_token not configured on device", token.isNotBlank())

        val api = HassanCloudApi(client)
        val project = api.createProject(baseUrl, token, "Android Build Smoke ${System.currentTimeMillis()}", "")
            .getOrThrow()
        val job = api.createJob(
            baseUrl = baseUrl,
            token = token,
            projectId = project.id,
            conversationId = null,
            goal = "build isolated Android fixture APK",
            jobType = "android_build",
            idempotencyKey = "android-build-smoke-${project.id}",
        ).getOrThrow()

        var final = job
        for (attempt in 0 until 120) {
            delay(5_000)
            final = api.getJob(baseUrl, token, job.id).getOrThrow()
            if (final.state in listOf("COMPLETED", "FAILED", "CANCELLED")) break
        }
        assertEquals("COMPLETED", final.state)

        val artifacts = api.listArtifacts(baseUrl, token, project.id).getOrThrow()
        val apk = artifacts.firstOrNull { it.name.endsWith(".apk") }
        assertTrue("APK artifact missing: ${artifacts.map { it.name }}", apk != null)
        val destination = File(context.cacheDir, apk!!.name)
        val downloaded = api.downloadArtifact(baseUrl, token, apk.id, destination).getOrThrow()
        assertTrue("APK is unexpectedly small", downloaded.length() > 1_024)
        val signature = downloaded.inputStream().use { byteArrayOf(it.read().toByte(), it.read().toByte()) }
        assertTrue("Downloaded artifact is not a ZIP/APK", signature.contentEquals(byteArrayOf(0x50, 0x4B)))
        ZipFile(downloaded).use { apkZip ->
            assertTrue("APK has no AndroidManifest.xml", apkZip.getEntry("AndroidManifest.xml") != null)
            assertTrue("APK has no classes.dex", apkZip.getEntry("classes.dex") != null)
        }
    }
}
