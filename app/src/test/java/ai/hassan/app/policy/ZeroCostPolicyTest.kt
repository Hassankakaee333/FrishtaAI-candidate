package ai.hassan.app.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZeroCostPolicyTest {
    @Test
    fun freeAndPrepaidWithZeroAdditionalCostAreAllowed() {
        assertTrue(ZeroCostPolicy.evaluate(SpendRequest("free", CostClass.FREE, 0)).allowed)
        assertTrue(ZeroCostPolicy.evaluate(SpendRequest("subscription", CostClass.PREPAID, 0)).allowed)
    }

    @Test
    fun meteredCardAndPositiveSpendAreBlocked() {
        assertFalse(ZeroCostPolicy.evaluate(SpendRequest("metered", CostClass.METERED, 0)).allowed)
        assertFalse(ZeroCostPolicy.evaluate(SpendRequest("card", CostClass.FREE, 0, requiresCard = true)).allowed)
        assertFalse(ZeroCostPolicy.evaluate(SpendRequest("cost", CostClass.FREE, 1)).allowed)
    }
}
