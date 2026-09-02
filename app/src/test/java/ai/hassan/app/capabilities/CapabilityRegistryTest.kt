package ai.hassan.app.capabilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CapabilityRegistryTest {
    @Test
    fun normalChatCapabilityExists() {
        val cap = CapabilityRegistry.find("NORMAL_CHAT")
        assertNotNull(cap)
        assertEquals(CapabilityStatus.NOT_CONFIGURED, cap?.status)
    }

    @Test
    fun radarDiscoveryIsWorking() {
        val cap = CapabilityRegistry.find("RADAR_DISCOVERY")
        assertEquals(CapabilityStatus.WORKING, cap?.status)
    }
}
