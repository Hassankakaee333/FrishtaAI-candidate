package ai.hassan.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareIngestionInstrumentedTest {
    @Test
    fun sharedTextBecomesLocalDraftTask() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<HassanApplication>()
        val marker = "share-test-${UUID.randomUUID()}"

        application.container.repository.ingestShare(
            text = "https://example.com/$marker",
            imageUri = null,
        )

        val task = withTimeout(5_000) {
            application.container.repository.tasks.first { tasks ->
                tasks.any { it.payload.contains(marker) }
            }.first { it.payload.contains(marker) }
        }
        assertEquals("SHARE_SHEET", task.source)
        assertTrue(task.payload.contains(marker))
    }
}
