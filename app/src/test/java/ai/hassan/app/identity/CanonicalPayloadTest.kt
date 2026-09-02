package ai.hassan.app.identity

import org.junit.Assert.assertEquals
import org.junit.Test

class CanonicalPayloadTest {
    @Test
    fun decisionPayloadIsStableAndSorted() {
        val payload = CanonicalPayload.decision(
            decisionId = "decision-1",
            action = "APPROVE",
            timestamp = 123L,
        )

        assertEquals(
            """{"action":"APPROVE","decisionId":"decision-1","timestamp":123,"version":1}""",
            payload,
        )
    }
}
