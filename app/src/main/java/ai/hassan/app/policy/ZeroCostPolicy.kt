package ai.hassan.app.policy

enum class CostClass {
    FREE,
    PREPAID,
    METERED,
}

data class SpendRequest(
    val providerId: String,
    val costClass: CostClass,
    val estimatedAdditionalCostCents: Long,
    val requiresCard: Boolean = false,
    val enablesAutoTopUp: Boolean = false,
)

data class PolicyDecision(
    val allowed: Boolean,
    val reason: String,
)

/** Immutable runtime law. Agents receive no mutator for these values. */
object ZeroCostPolicy {
    const val additionalSpendLimitCents: Long = 0
    const val paidApiAllowed: Boolean = false
    const val paidGpuAllowed: Boolean = false
    const val autoTopUp: Boolean = false
    const val agentCanChangeBudget: Boolean = false

    fun evaluate(request: SpendRequest): PolicyDecision {
        if (request.costClass == CostClass.METERED) {
            return PolicyDecision(false, "METERED resources are blocked by the constitution.")
        }
        if (request.estimatedAdditionalCostCents > additionalSpendLimitCents) {
            return PolicyDecision(false, "Additional spending must remain exactly zero.")
        }
        if (request.requiresCard) {
            return PolicyDecision(false, "Resources requiring a payment card are blocked.")
        }
        if (request.enablesAutoTopUp) {
            return PolicyDecision(false, "Automatic top-up is blocked.")
        }
        return PolicyDecision(true, "Allowed by zero-cost policy.")
    }
}
