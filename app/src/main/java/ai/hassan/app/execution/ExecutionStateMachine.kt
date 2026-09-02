package ai.hassan.app.execution

enum class ExecutionState {
    DISCUSSING,
    PLAN_READY,
    AWAITING_USER_APPROVAL,
    QUEUED,
    EXECUTING,
    NEEDS_INPUT,
    VERIFYING,
    FAILED,
    CANDIDATE_READY,
    APPROVED,
    REJECTED,
    COMPLETED,
}

enum class ExecutionEvent {
    PLAN_CREATED,
    REQUEST_APPROVAL,
    USER_APPROVED,
    USER_REJECTED,
    START_EXECUTION,
    REQUEST_INPUT,
    INPUT_RECEIVED,
    START_VERIFICATION,
    CANDIDATE_BUILT,
    COMPLETE,
    FAIL,
    APPROVE_CANDIDATE,
}

object ApprovalPhraseParser {
    private val exactPhrases = setOf(
        "ابدأ",
        "ابدا",
        "نفذ",
        "نفّذ",
        "اعتمد",
        "موافق",
        "ابدأ التنفيذ",
        "ابدا التنفيذ",
        "ابدأ الآن",
        "ابدا الان",
        "ابدا الآن",
        "نفذ الآن",
        "نفّذ الآن",
        "نفذ الخطة",
        "نفّذ الخطة",
        "قم بالتنفيذ",
        "ابدأ البناء",
        "ابدا البناء",
    )

    fun isApproval(text: String): Boolean {
        val n = normalize(text)
        if (n in exactPhrases) return true
        // Short confirmation variants while a plan is waiting.
        if (n == "يلا" || n == "يلا ابدأ" || n == "يلا ابدا") return true
        if (n.startsWith("ابدأ") || n.startsWith("ابدا") || n.startsWith("نفذ") || n.startsWith("نفّذ")) {
            if (n.length <= 24) return true
        }
        return false
    }

    private fun normalize(text: String): String = text
        .trim()
        .replace(Regex("[.!؟،]+$"), "")
        .replace(Regex("\\s+"), " ")
}

object ExecutionStateMachine {
    fun transition(current: ExecutionState, event: ExecutionEvent): ExecutionState = when (current to event) {
        ExecutionState.DISCUSSING to ExecutionEvent.PLAN_CREATED -> ExecutionState.PLAN_READY
        ExecutionState.PLAN_READY to ExecutionEvent.REQUEST_APPROVAL -> ExecutionState.AWAITING_USER_APPROVAL
        ExecutionState.AWAITING_USER_APPROVAL to ExecutionEvent.USER_APPROVED -> ExecutionState.QUEUED
        ExecutionState.AWAITING_USER_APPROVAL to ExecutionEvent.USER_REJECTED -> ExecutionState.REJECTED
        ExecutionState.QUEUED to ExecutionEvent.START_EXECUTION -> ExecutionState.EXECUTING
        ExecutionState.EXECUTING to ExecutionEvent.REQUEST_INPUT -> ExecutionState.NEEDS_INPUT
        ExecutionState.NEEDS_INPUT to ExecutionEvent.INPUT_RECEIVED -> ExecutionState.VERIFYING
        ExecutionState.EXECUTING to ExecutionEvent.START_VERIFICATION -> ExecutionState.VERIFYING
        ExecutionState.VERIFYING to ExecutionEvent.CANDIDATE_BUILT -> ExecutionState.CANDIDATE_READY
        ExecutionState.VERIFYING to ExecutionEvent.COMPLETE -> ExecutionState.COMPLETED
        ExecutionState.CANDIDATE_READY to ExecutionEvent.APPROVE_CANDIDATE -> ExecutionState.APPROVED
        else -> when (event) {
            ExecutionEvent.FAIL -> ExecutionState.FAILED
            else -> current
        }
    }
}
