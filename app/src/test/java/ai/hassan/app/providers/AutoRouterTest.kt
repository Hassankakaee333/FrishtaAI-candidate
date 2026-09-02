package ai.hassan.app.providers

import ai.hassan.app.conversation.ConversationIntent
import ai.hassan.app.policy.CostClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoRouterTest {
    @Test
    fun researchUsesFreeOfficialRadarFirst() {
        val provider = DeterministicAutoRouter.route(
            "ابحث في الرادار عن مصادر مجانية",
            ConversationIntent.RESEARCH,
        )
        assertEquals("official-radar", provider?.id)
        assertEquals(CostClass.FREE, provider?.costClass)
    }

    @Test
    fun codingFallsBackToExistingHumanGatedSubscriptionNotMeteredApi() {
        val provider = DeterministicAutoRouter.route(
            "ابن تطبيق Android واختبره",
            ConversationIntent.EXECUTION,
        )
        assertEquals("chatgpt", provider?.id)
        assertEquals("gpt-5.6-sol", provider?.modelId)
        assertNotEquals(CostClass.METERED, provider?.costClass)
    }

    @Test
    fun codexTaskPackCarriesExactModelAndSelectedReasoningEffort() {
        val provider = HumanGatedLeadBrain(
            ProviderCatalog.chatGpt,
            CodexReasoningEffort.HIGH,
        )
        val pack = provider.buildTaskPack(
            LeadBrainContext("conversation", "ابن التطبيق", ai.hassan.app.execution.ExecutionState.EXECUTING),
            "task",
        )

        assertEquals("gpt-5.6-sol", pack.requestedModel)
        assertEquals("high", pack.reasoningEffort)
        assertTrue(pack.asShareText().contains("Requested model: gpt-5.6-sol"))
        assertTrue(pack.asShareText().contains("Requested reasoning effort: high"))
    }
}
