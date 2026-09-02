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
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

@RunWith(AndroidJUnit4::class)
class CloudPersistentWorkspaceSmokeTest {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private suspend fun waitForJob(api: HassanCloudApi, baseUrl: String, token: String, id: String): CloudJobDto {
        var job = api.getJob(baseUrl, token, id).getOrThrow()
        repeat(80) {
            if (job.state in listOf("COMPLETED", "FAILED", "CANCELLED")) return job
            delay(3_000)
            job = api.getJob(baseUrl, token, id).getOrThrow()
        }
        return job
    }

    @Test
    fun persistentWorkspaceSurvivesAcrossCloudJobs() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("hassan_conversation_settings", Context.MODE_PRIVATE)
        val baseUrl = prefs.getString("cloud_base_url", "").orEmpty().trim()
        val token = prefs.getString("access_token", "").orEmpty().trim()
        assertTrue(baseUrl.startsWith("https://"))
        assertTrue(token.isNotBlank())

        val api = HassanCloudApi(client)
        assertEquals("ok", api.health(baseUrl).getOrThrow().status)
        val project = api.createProject(
            baseUrl,
            token,
            "Phone Persistent ${System.currentTimeMillis()}",
            "persistent workspace device test",
        ).getOrThrow()

        val appSource = "def greet(name: str) -> str:\n    return f'hello {name}'\n"
        val testSource = "from app import greet\n\ndef test_greet():\n    assert greet('hassan') == 'hello hassan'\n"
        api.putWorkspaceFile(baseUrl, token, project.id, "app.py", appSource.encodeToByteArray()).getOrThrow()
        api.putWorkspaceFile(baseUrl, token, project.id, "test_app.py", testSource.encodeToByteArray()).getOrThrow()
        assertEquals(2, api.listWorkspaceFiles(baseUrl, token, project.id).getOrThrow().size)

        val job1 = api.createJob(
            baseUrl, token, project.id, null,
            "first persistent workspace edit", "workspace_coding",
            "phone-pws-1-${System.currentTimeMillis()}",
        ).getOrThrow()
        val final1 = waitForJob(api, baseUrl, token, job1.id)
        assertEquals("COMPLETED", final1.state)

        val after1 = api.getWorkspaceFile(baseUrl, token, project.id, "app.py").getOrThrow()
        val text1 = Base64.getDecoder().decode(after1.content_base64).decodeToString()
        assertEquals(1, Regex("Hassan job ").findAll(text1).count())

        val job2 = api.createJob(
            baseUrl, token, project.id, null,
            "second persistent workspace edit", "workspace_coding",
            "phone-pws-2-${System.currentTimeMillis()}",
        ).getOrThrow()
        val final2 = waitForJob(api, baseUrl, token, job2.id)
        assertEquals("COMPLETED", final2.state)

        val after2 = api.getWorkspaceFile(baseUrl, token, project.id, "app.py").getOrThrow()
        val text2 = Base64.getDecoder().decode(after2.content_base64).decodeToString()
        assertEquals(2, Regex("Hassan job ").findAll(text2).count())

        val artifacts = api.listArtifacts(baseUrl, token, project.id).getOrThrow()
        val workspaceArtifact = artifacts.lastOrNull { it.job_id == job2.id && it.name == "workspace.zip" }
        assertTrue(workspaceArtifact != null)

        val downloaded = File(context.cacheDir, "persistent-${job2.id}.zip")
        api.downloadArtifact(baseUrl, token, workspaceArtifact!!.id, downloaded).getOrThrow()
        assertTrue(downloaded.length() > 0)

        ZipFile(downloaded).use { zip ->
            val appEntry = zip.getEntry("app.py")
            val testEntry = zip.getEntry("test_app.py")
            assertTrue(appEntry != null)
            assertTrue(testEntry != null)
            val zippedText = zip.getInputStream(appEntry).bufferedReader().readText()
            assertEquals(2, Regex("Hassan job ").findAll(zippedText).count())
        }

        assertEquals(2, api.listWorkspaceFiles(baseUrl, token, project.id).getOrThrow().size)
    }
}
