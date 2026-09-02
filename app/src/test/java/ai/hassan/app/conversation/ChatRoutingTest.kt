package ai.hassan.app.conversation

import ai.hassan.app.providers.Capability
import ai.hassan.app.providers.DeterministicAutoRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatRoutingTest {
    @Test fun chatIntentDoesNotRouteToProvider() {
        assertNull(DeterministicAutoRouter.route("مرحبا", ConversationIntent.CHAT))
        assertEquals(Capability.CHAT, DeterministicAutoRouter.classify("مرحبا", ConversationIntent.CHAT))
    }

    @Test fun researchIntentRoutesToRadar() {
        val provider = DeterministicAutoRouter.route(
            "ابحث عن آخر إصدار من Android",
            ConversationIntent.RESEARCH,
        )
        assertEquals("official-radar", provider?.id)
    }

    @Test fun executionCodingRoutesToChatGpt() {
        val provider = DeterministicAutoRouter.route("عدل كود التطبيق", ConversationIntent.EXECUTION)
        assertEquals("chatgpt", provider?.id)
    }

    @Test fun appBenefitDoesNotRouteToCoding() {
        assertNotEquals(
            Capability.CODING,
            DeterministicAutoRouter.classify("ما فائدة تطبيق حسن؟", ConversationIntent.CHAT),
        )
        assertNull(DeterministicAutoRouter.route("ما فائدة تطبيق حسن؟", ConversationIntent.CHAT))
    }

    @Test fun explainKotlinIsChatCapability() {
        assertEquals(Capability.CHAT, DeterministicAutoRouter.classify("اشرح لي Kotlin", ConversationIntent.CHAT))
    }
}
