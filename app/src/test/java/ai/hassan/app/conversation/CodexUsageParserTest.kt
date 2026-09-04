package ai.hassan.app.conversation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodexUsageParserTest {
    @Test
    fun parsesOnlyWindowsActuallyReturnedAndConvertsUsedToRemaining() {
        val payload = Json.parseToJsonElement(
            """
            {
              "primary": { "used_percent": 27.0, "window_minutes": 300 },
              "daily": { "remaining_percent": 81 },
              "secondary": { "usedPercent": 42, "windowDurationMins": 10080 },
              "monthly": { "used_percent": 9 }
            }
            """.trimIndent(),
        )

        val result = CodexUsageParser.parse(payload)

        assertEquals(
            listOf(
                CodexUsageWindow("5 ساعات", 73),
                CodexUsageWindow("يومي", 81),
                CodexUsageWindow("أسبوعي", 58),
                CodexUsageWindow("شهري", 91),
            ),
            result?.windows,
        )
    }

    @Test
    fun supportsReturnedWindowArraysAndOmitsUnknownDurations() {
        val payload = Json.parseToJsonElement(
            """
            { "windows": [
              { "used_percent": 10, "window_minutes": 1440 },
              { "used_percent": 20, "window_minutes": 60 }
            ] }
            """.trimIndent(),
        )

        assertEquals(listOf(CodexUsageWindow("يومي", 90)), CodexUsageParser.parse(payload)?.windows)
    }

    @Test
    fun returnsNullWhenThereIsNoReading() {
        assertNull(CodexUsageParser.parse(Json.parseToJsonElement("{}")))
    }
}
