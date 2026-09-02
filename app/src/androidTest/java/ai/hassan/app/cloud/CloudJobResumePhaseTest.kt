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

/** Re-enters in a fresh process and verifies the server-owned job and artifact. */
@RunWith(AndroidJUnit4::class)
class CloudJobResumePhaseTest {
    @Test
    fun resumeJobAfterClientProcessWasStopped() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cloudPrefs = context.getSharedPreferences("hassan_conversation_settings", Context.MODE_PRIVATE)
        val baseUrl = cloudPrefs.getString("cloud_base_url", "").orEmpty().trim()
        val token = cloudPrefs.getString("access_token", "").orEmpty().trim()
        val resumePrefs = context.getSharedPreferences("hassan_close_resume_test", Context.MODE_PRIVATE)
        val projectId = resumePrefs.getString("project_id", "").orEmpty()
        val jobId = resumePrefs.getString("job_id", "").orEmpty()
        assertTrue("close phase project ID missing", projectId.isNotBlank())
        assertTrue("close phase job ID missing", jobId.isNotBlank())

        val api = HassanCloudApi(
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build(),
        )
        var final = api.getJob(baseUrl, token, jobId).getOrThrow()
        for (attempt in 0 until 120) {
            if (final.state in listOf("COMPLETED", "FAILED", "CANCELLED")) break
            delay(5_000)
            final = api.getJob(baseUrl, token, jobId).getOrThrow()
        }
        assertEquals("COMPLETED", final.state)

        val artifacts = api.listArtifacts(baseUrl, token, projectId).getOrThrow()
        val workspace = artifacts.firstOrNull { it.name == "workspace.zip" }
        assertTrue("workspace artifact missing after resume", workspace != null)
        val destination = File(context.cacheDir, "resumed-${workspace!!.name}")
        val downloaded = api.downloadArtifact(baseUrl, token, workspace.id, destination).getOrThrow()
        assertTrue("resumed artifact is empty", downloaded.length() > 0)
    }
}
