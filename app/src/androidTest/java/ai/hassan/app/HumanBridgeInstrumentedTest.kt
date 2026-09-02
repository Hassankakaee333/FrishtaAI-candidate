package ai.hassan.app

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ai.hassan.app.providers.HumanBridgeLauncher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HumanBridgeInstrumentedTest {
    @Test
    fun bridgeUsesOfficialActionSendWithoutAutomation() {
        val context = ApplicationProvider.getApplicationContext<HassanApplication>()
        val chooser = HumanBridgeLauncher(context).createIntent("chatgpt", "TaskPack marker")
        val target = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            ?: error("Chooser target missing")
        assertEquals(Intent.ACTION_SEND, target.action)
        assertEquals("text/plain", target.type)
        assertTrue(target.getStringExtra(Intent.EXTRA_TEXT)?.contains("TaskPack marker") == true)
    }
}
