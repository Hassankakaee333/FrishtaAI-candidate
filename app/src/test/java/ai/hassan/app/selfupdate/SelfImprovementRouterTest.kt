package ai.hassan.app.selfupdate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelfImprovementRouterTest {
    @Test
    fun modifyAppSectionTriggersSelfImprove() {
        assertEquals(
            SelfImprovementAction.REQUEST_SELF_IMPROVE,
            SelfImprovementRouter.classify("عدّل جزء من واجهة التطبيق"),
        )
    }

    @Test
    fun improveAppTriggersSelfImprove() {
        assertEquals(
            SelfImprovementAction.REQUEST_SELF_IMPROVE,
            SelfImprovementRouter.classify("حسّن التطبيق"),
        )
    }

    @Test
    fun updateHassanTriggersUpdate() {
        assertEquals(
            SelfImprovementAction.APPLY_UPDATE,
            SelfImprovementRouter.classify("حدّث حسن"),
        )
    }

    @Test
    fun installUpdateTriggersUpdate() {
        assertEquals(
            SelfImprovementAction.APPLY_UPDATE,
            SelfImprovementRouter.classify("ثبت التحديث"),
        )
    }

    @Test
    fun rollbackPhraseWorks() {
        assertEquals(
            SelfImprovementAction.ROLLBACK,
            SelfImprovementRouter.classify("ارجع للنسخة السابقة"),
        )
    }

    @Test
    fun normalChatIsNotSelfUpdate() {
        assertNull(SelfImprovementRouter.classify("مرحبا"))
    }
}
