package ai.hassan.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ai.hassan.app.data.RadarStatuses
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadarInstrumentedTest {
    @Test
    fun realOfficialRadarPersistsAtLeastOneVerifiedFreeResult() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<HassanApplication>()
        application.container.repository.runRadarNow()
        val findings = withTimeout(70_000) {
            application.container.repository.radarFindings.first { rows ->
                rows.any { it.status == RadarStatuses.VERIFIED }
            }
        }
        assertTrue(findings.any { it.status == RadarStatuses.VERIFIED && it.costClass == "FREE" })
    }
}
