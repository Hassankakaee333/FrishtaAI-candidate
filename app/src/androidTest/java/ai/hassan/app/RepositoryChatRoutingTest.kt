package ai.hassan.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryChatRoutingTest {
    @Test
    fun chatMessageDoesNotCreateExecutionPlan() = runBlocking {
        val app = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as HassanApplication
        val repository = app.container.repository
        val conversation = InstrumentedTestSupport.isolatedConversation(repository, "chat-plan-test")
        val plansBefore = InstrumentedTestSupport.planIdsForConversation(repository, conversation.id)

        repository.sendChat("مرحبا")

        val newPlans = repository.plans.first()
            .filter { it.conversationId == conversation.id && it.id !in plansBefore }
            .filter { it.status !in setOf("REJECTED", "COMPLETED") }
        assertTrue("Normal chat must not create ExecutionPlan", newPlans.isEmpty())
    }

    @Test
    fun chatMessageKeepsDiscussingState() = runBlocking {
        val app = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as HassanApplication
        val repository = app.container.repository
        val conversation = InstrumentedTestSupport.isolatedConversation(repository, "discussing-test")

        repository.sendChat("ما إمكانياتك؟")

        val updated = repository.conversations.first().firstOrNull { it.id == conversation.id }
        assertEquals("DISCUSSING", updated?.state)
    }

    @Test
    fun unconfiguredChatDoesNotFallBackToRadarPlan() = runBlocking {
        val app = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as HassanApplication
        val repository = app.container.repository
        val conversation = InstrumentedTestSupport.isolatedConversation(repository, "radar-fallback-test")
        val plansBefore = InstrumentedTestSupport.planIdsForConversation(repository, conversation.id)

        repository.sendChat("مرحبا")

        val newPlans = repository.plans.first()
            .filter { it.conversationId == conversation.id && it.id !in plansBefore }
        val radarPlans = newPlans.filter {
            it.capability == "RESEARCH" || it.summary.contains("رادار")
        }
        assertTrue("Chat must not fall back to radar ExecutionPlan", radarPlans.isEmpty())
    }
}
