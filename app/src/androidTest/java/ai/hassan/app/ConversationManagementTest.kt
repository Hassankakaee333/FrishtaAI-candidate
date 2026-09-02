package ai.hassan.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationManagementTest {
    @Test
    fun newChatCreatesSeparateConversation() = runBlocking {
        val app = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as HassanApplication
        val repository = app.container.repository
        repository.initialize()

        val before = repository.conversations.first().size
        val created = repository.createNewConversation()
        val after = repository.conversations.first()

        assertEquals(before + 1, after.size)
        assertEquals(created.id, repository.activeConversationId.first())
    }

    @Test
    fun selectConversationSwitchesActiveId() = runBlocking {
        val app = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as HassanApplication
        val repository = app.container.repository

        val first = repository.createNewConversation("أولى")
        val second = repository.createNewConversation("ثانية")
        repository.selectConversation(first.id)

        assertEquals(first.id, repository.activeConversationId.first())
        repository.selectConversation(second.id)
        assertEquals(second.id, repository.activeConversationId.first())
        assertNotEquals(first.id, second.id)
    }
}
