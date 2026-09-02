package ai.hassan.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskStatusesTest {
    @Test
    fun milestoneOneStatusesRemainExplicit() {
        val values = setOf(
            TaskStatuses.DRAFT,
            TaskStatuses.QUEUED,
            TaskStatuses.RUNNING,
            TaskStatuses.WAITING_DECISION,
            TaskStatuses.APPROVED,
            TaskStatuses.REJECTED,
        )

        assertEquals(6, values.size)
        assertTrue(values.contains("WAITING_DECISION"))
    }
}
