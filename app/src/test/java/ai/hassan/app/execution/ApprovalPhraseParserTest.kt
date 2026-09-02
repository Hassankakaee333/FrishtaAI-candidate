package ai.hassan.app.execution

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApprovalPhraseParserTest {
    @Test
    fun acceptsAbdaWithoutHamza() {
        assertTrue(ApprovalPhraseParser.isApproval("ابدا"))
        assertTrue(ApprovalPhraseParser.isApproval("ابدا التنفيذ"))
    }

    @Test
    fun acceptsStartWithHamza() {
        assertTrue(ApprovalPhraseParser.isApproval("ابدأ"))
        assertTrue(ApprovalPhraseParser.isApproval("ابدأ التنفيذ"))
    }

    @Test
    fun rejectsNormalChat() {
        assertFalse(ApprovalPhraseParser.isApproval("هل بدات التنفيذ؟"))
        assertFalse(ApprovalPhraseParser.isApproval("ما إمكانياتك؟"))
    }
}
