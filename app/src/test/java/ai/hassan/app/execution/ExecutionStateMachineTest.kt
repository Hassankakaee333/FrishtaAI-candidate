package ai.hassan.app.execution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionStateMachineTest {
    @Test
    fun approvalPhraseIsExactAndOnlyWorksAtApprovalGate() {
        assertTrue(ApprovalPhraseParser.isApproval("ابدأ التنفيذ"))
        assertTrue(ApprovalPhraseParser.isApproval("نفّذ!"))
        assertFalse(ApprovalPhraseParser.isApproval("ربما ابدأ لاحقًا"))
        assertEquals(
            ExecutionState.DISCUSSING,
            ExecutionStateMachine.transition(ExecutionState.DISCUSSING, ExecutionEvent.USER_APPROVED),
        )
        assertEquals(
            ExecutionState.QUEUED,
            ExecutionStateMachine.transition(ExecutionState.AWAITING_USER_APPROVAL, ExecutionEvent.USER_APPROVED),
        )
    }

    @Test
    fun planApprovalExecutionAndVerificationFollowClosedTransitions() {
        val plan = ExecutionStateMachine.transition(ExecutionState.DISCUSSING, ExecutionEvent.PLAN_CREATED)
        val waiting = ExecutionStateMachine.transition(plan, ExecutionEvent.REQUEST_APPROVAL)
        val queued = ExecutionStateMachine.transition(waiting, ExecutionEvent.USER_APPROVED)
        val running = ExecutionStateMachine.transition(queued, ExecutionEvent.START_EXECUTION)
        val verifying = ExecutionStateMachine.transition(running, ExecutionEvent.START_VERIFICATION)
        val completed = ExecutionStateMachine.transition(verifying, ExecutionEvent.COMPLETE)
        assertEquals(ExecutionState.COMPLETED, completed)
    }
}
