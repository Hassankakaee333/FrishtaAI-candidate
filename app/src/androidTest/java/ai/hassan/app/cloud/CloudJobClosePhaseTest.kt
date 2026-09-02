package ai.hassan.app.cloud

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Starts a real job and persists only its IDs before the client process exits. */
@RunWith(AndroidJUnit4::class)
class CloudJobClosePhaseTest {
    @Test
    fun startJobBeforeClientCloses() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cloudPrefs = context.getSharedPreferences("hassan_conversation_settings", Context.MODE_PRIVATE)
        val baseUrl = cloudPrefs.getString("cloud_base_url", "").orEmpty().trim()
        val token = cloudPrefs.getString("access_token", "").orEmpty().trim()
        assertTrue("cloud_base_url not configured on device", baseUrl.startsWith("https://"))
        assertTrue("access_token not configured on device", token.isNotBlank())

        val api = HassanCloudApi(
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build(),
        )
        val project = api.createProject(baseUrl, token, "Close Resume ${System.currentTimeMillis()}", "")
            .getOrThrow()
        val job = api.createJob(
            baseUrl = baseUrl,
            token = token,
            projectId = project.id,
            conversationId = null,
            goal = "verify durable close and resume",
            jobType = "coding",
            idempotencyKey = "close-resume-${project.id}",
        ).getOrThrow()
        assertTrue(job.id.isNotBlank())
        assertTrue(
            context.getSharedPreferences("hassan_close_resume_test", Context.MODE_PRIVATE)
                .edit()
                .putString("project_id", project.id)
                .putString("job_id", job.id)
                .commit(),
        )
    }
}
