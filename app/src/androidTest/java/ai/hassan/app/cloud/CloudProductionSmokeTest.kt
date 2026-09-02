package ai.hassan.app.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Production cloud smoke test — runs on device over public Internet (no adb reverse).
 * Requires SharedPreferences cloud_base_url + access_token configured on device.
 */
@RunWith(AndroidJUnit4::class)
class CloudProductionSmokeTest {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Test
    fun productionCloudJobCompletesWithArtifacts() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("hassan_conversation_settings", android.content.Context.MODE_PRIVATE)
        val baseUrl = prefs.getString("cloud_base_url", "").orEmpty().trim()
        val token = prefs.getString("access_token", "").orEmpty().trim()
        assertTrue("cloud_base_url not configured on device", baseUrl.startsWith("https://"))
        assertTrue("access_token not configured on device", token.isNotBlank())

        val api = HassanCloudApi(client)
        val health = api.health(baseUrl).getOrThrow()
        assertEquals("ok", health.status)

        val project = api.createProject(baseUrl, token, "Device Smoke ${System.currentTimeMillis()}", "")
            .getOrThrow()
        val job = api.createJob(
            baseUrl = baseUrl,
            token = token,
            projectId = project.id,
            conversationId = null,
            goal = "device smoke coding job",
            jobType = "coding",
            idempotencyKey = "device-smoke-${project.id}",
        ).getOrThrow()
        assertTrue(job.state in listOf("QUEUED", "DISPATCHING"))

        var final = job
        for (attempt in 0 until 40) {
            kotlinx.coroutines.delay(5_000)
            final = api.getJob(baseUrl, token, job.id).getOrThrow()
            if (final.state in listOf("COMPLETED", "FAILED", "CANCELLED")) break
        }
        assertEquals("COMPLETED", final.state)

        val artifacts = api.listArtifacts(baseUrl, token, project.id).getOrThrow()
        assertTrue(artifacts.isNotEmpty())
        val first = artifacts.firstOrNull { it.name == "workspace.zip" } ?: artifacts.first()
        val dest = java.io.File(context.cacheDir, first.name)
        val downloaded = api.downloadArtifact(baseUrl, token, first.id, dest).getOrThrow()
        assertTrue(downloaded.length() > 0)
    }
}
