package ai.hassan.app.phoneagent

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneAgentModelTest {
    @Test
    fun actionSetContainsExpectedEntries() {
        assertTrue(PhoneAgentActions.supported.contains(PhoneAgentActions.OPEN_APP))
        assertTrue(PhoneAgentActions.supported.contains(PhoneAgentActions.UI_TREE))
        assertTrue(PhoneAgentActions.supported.contains(PhoneAgentActions.SCREENSHOT))
    }

    @Test
    fun commandSerializationRoundTrip() {
        val json = Json { encodeDefaults = true }
        val source = PhoneAgentCommand(
            id = "example-1",
            action = PhoneAgentActions.OPEN_APP,
            packageName = "example.package",
        )
        val decoded = json.decodeFromString<PhoneAgentCommand>(json.encodeToString(source))
        assertEquals(source, decoded)
    }
}
