package ai.hassan.app.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatFirstRoutingExtraTest {
    @Test
    fun developTodoAppIsExecution() {
        assertEquals(
            ConversationIntent.EXECUTION,
            IntentRouter.classify("طوّر تطبيق المهام وأضف الوضع الليلي"),
        )
    }

    @Test
    fun todoBenchmarkNightModeIsExecution() {
        assertEquals(
            ConversationIntent.EXECUTION,
            IntentRouter.classify("عدّل تطبيق HassanTodoBenchmark وأضف الوضع الليلي"),
        )
    }

    @Test
    fun databaseAdviceStaysChat() {
        assertEquals(
            ConversationIntent.CHAT,
            IntentRouter.classify("ما أفضل قاعدة بيانات لتطبيقي؟"),
        )
    }

    @Test
    fun researchPhraseIsResearchNotDefaultFallback() {
        assertEquals(
            ConversationIntent.RESEARCH,
            IntentRouter.classify("ابحث عن أفضل قاعدة بيانات"),
        )
        assertFalse(IntentRouter.classify("مرحبا") == ConversationIntent.RESEARCH)
    }

    @Test
    fun providerSelectorDefaultsAuto() {
        assertEquals("auto", ConversationSettings().chatProvider)
        assertTrue(ConversationSettingsStore.CHAT_PROVIDER_IDS.contains("claude"))
        assertTrue(ConversationSettingsStore.CHAT_PROVIDER_IDS.contains("auto"))
    }
}
