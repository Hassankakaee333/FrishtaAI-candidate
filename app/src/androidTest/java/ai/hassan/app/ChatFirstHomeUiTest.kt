package ai.hassan.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ChatFirstHomeUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun unlock() {
        composeRule.activity.setShowWhenLocked(true)
        composeRule.activity.setTurnScreenOn(true)
        composeRule.waitForIdle()
    }

    @Test
    fun homeOpensChatWithComposerAndProviderSelector() {
        composeRule.onNodeWithTag("chat_home").assertIsDisplayed()
        composeRule.onNodeWithTag("composer").assertIsDisplayed()
        composeRule.onNodeWithTag("provider_selector").assertIsDisplayed()
        composeRule.onNodeWithTag("provider_status_dot").assertIsDisplayed()
        composeRule.onNodeWithTag("connection_status_label").assertIsDisplayed()
    }

    @Test
    fun providerDefaultsFrishtaAuto() {
        composeRule.onNodeWithTag("provider_selector").assertIsDisplayed()
        composeRule.onNodeWithText("Frishta Auto").assertIsDisplayed()
    }

    @Test
    fun greetingDoesNotShowTaskCard() {
        val app = composeRule.activity.application as HassanApplication
        val beforeIds = runBlocking {
            app.container.repository.messages.first().map { it.id }.toSet()
        }
        composeRule.onNodeWithTag("composer").performTextInput("كيف حالك؟")
        composeRule.onNodeWithContentDescription("إرسال").performClick()
        composeRule.waitForIdle()
        Thread.sleep(800)
        val hasPlan = runCatching {
            composeRule.onNodeWithTag("plan_card").assertIsDisplayed()
            true
        }.getOrDefault(false)
        assertFalse(hasPlan)
        val newLinked = runBlocking {
            app.container.repository.messages.first()
                .filter { it.id !in beforeIds }
                .any { !it.taskId.isNullOrBlank() }
        }
        assertFalse("Greeting must not create linked cloud task message", newLinked)
    }

    @Test
    fun secondaryNavStillReachable() {
        composeRule.onNodeWithContentDescription("القائمة").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("nav_Projects"), 5_000)
        composeRule.onNodeWithTag("nav_Projects").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sync_cloud_projects").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("القائمة").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("nav_Settings"), 5_000)
        composeRule.onNodeWithTag("nav_Settings").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("settings_scroll"), 5_000)
        composeRule.onNodeWithTag("settings_scroll").assertIsDisplayed()
    }

    @Test
    fun newChatResetsProjectContextChipAbsence() {
        composeRule.onNodeWithContentDescription("القائمة").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("new_chat"), 5_000)
        composeRule.onNodeWithTag("new_chat").performClick()
        composeRule.waitForIdle()
        val hasChip = runCatching {
            composeRule.onNodeWithTag("project_context_chip").assertIsDisplayed()
            true
        }.getOrDefault(false)
        assertFalse("New chat should not show project context until bound", hasChip)
        val conversation = runBlocking {
            val app = composeRule.activity.application as HassanApplication
            val id = app.container.repository.activeConversationId.first()
            app.container.repository.conversations.first().first { it.id == id }
        }
        assertEquals("", conversation.projectId)
    }
}
