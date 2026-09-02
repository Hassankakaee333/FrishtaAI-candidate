package ai.hassan.app.conversation

import org.junit.Assert.assertEquals
import org.junit.Test

class IntentRouterTest {
    @Test fun appBenefitQuestionIsChat() {
        assertEquals(ConversationIntent.CHAT, IntentRouter.classify("ما فائدة تطبيق حسن؟"))
    }

    @Test fun explainKotlinIsChat() {
        assertEquals(ConversationIntent.CHAT, IntentRouter.classify("اشرح لي Kotlin"))
    }

    @Test fun deviceDiagnosticIsExecution() {
        assertEquals(ConversationIntent.EXECUTION, IntentRouter.classify("افحص الجهاز الآن"))
    }

    @Test fun mediaRequestIsMediaNotExecution() {
        assertEquals(ConversationIntent.MEDIA, IntentRouter.classify("اصنع لي صورة"))
    }

    @Test fun greetingIsChat() {
        assertEquals(ConversationIntent.CHAT, IntentRouter.classify("مرحبا"))
    }

    @Test fun capabilitiesQuestionIsChat() {
        assertEquals(ConversationIntent.CHAT, IntentRouter.classify("ما إمكانياتك؟"))
    }

    @Test fun identityQuestionIsChat() {
        assertEquals(ConversationIntent.CHAT, IntentRouter.classify("من أنت؟"))
    }

    @Test fun explainAppQuestionIsChat() {
        assertEquals(ConversationIntent.CHAT, IntentRouter.classify("اشرح لي تطبيق حسن"))
    }

    @Test fun comparisonQuestionIsChat() {
        assertEquals(ConversationIntent.CHAT, IntentRouter.classify("ما الفرق بين Gemini و ChatGPT؟"))
    }

    @Test fun howAreYouIsChat() {
        assertEquals(ConversationIntent.CHAT, IntentRouter.classify("كيف حالك؟"))
    }

    @Test fun explainIdeaIsChat() {
        assertEquals(ConversationIntent.CHAT, IntentRouter.classify("اشرح لي هذه الفكرة"))
    }

    @Test fun editCodeIsExecution() {
        assertEquals(ConversationIntent.EXECUTION, IntentRouter.classify("عدل كود التطبيق"))
    }

    @Test fun searchAndroidVersionIsResearch() {
        assertEquals(ConversationIntent.RESEARCH, IntentRouter.classify("ابحث عن آخر إصدار من Android"))
    }

    @Test fun executeModificationIsExecution() {
        assertEquals(ConversationIntent.EXECUTION, IntentRouter.classify("نفذ التعديل"))
    }

    @Test fun startExecutionPhraseIsExecution() {
        assertEquals(ConversationIntent.EXECUTION, IntentRouter.classify("ابدأ التنفيذ"))
    }

    @Test fun phoneDiagnosticIsExecution() {
        assertEquals(ConversationIntent.EXECUTION, IntentRouter.classify("افحص الهاتف وابحث عن مشكلة"))
    }

    @Test fun unknownDefaultsToChat() {
        assertEquals(ConversationIntent.CHAT, IntentRouter.classify("هذا نص عام بدون طلب تنفيذ"))
    }
}
