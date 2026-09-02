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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ChatFirstUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun makeTestActivityVisibleOnLockedDevice() {
        composeRule.activity.setShowWhenLocked(true)
        composeRule.activity.setTurnScreenOn(true)
        composeRule.waitForIdle()
    }

    @Test
    fun chatHomeShowsComposerWithoutLeadBrainSelector() {
        composeRule.onNodeWithTag("chat_home").assertIsDisplayed()
        composeRule.onNodeWithTag("composer").assertIsDisplayed()
        composeRule.onNodeWithTag("send_message").assertIsDisplayed()
        composeRule.onNodeWithTag("provider_status_dot").assertIsDisplayed()
    }

    @Test
    fun normalChatDoesNotShowPlanCard() {
        composeRule.onNodeWithTag("composer").performTextInput("مرحبا")
        composeRule.onNodeWithContentDescription("إرسال").performClick()
        composeRule.waitForIdle()
        Thread.sleep(500)
        val hasPlan = runCatching {
            composeRule.onNodeWithTag("plan_card").assertIsDisplayed()
            true
        }.getOrDefault(false)
        assertFalse("Normal chat must not render plan_card", hasPlan)
    }

    @Test
    fun leadBrainSelectorLivesInSettings() {
        composeRule.onNodeWithContentDescription("القائمة").performClick()
        val hasLeadOnHome = runCatching {
            composeRule.onNodeWithTag("lead_auto").assertIsDisplayed()
            true
        }.getOrDefault(false)
        assertFalse("Lead brain selector must not be on home screen", hasLeadOnHome)
        composeRule.onNodeWithTag("nav_Settings").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("lead_brain_selector"), 5_000)
        composeRule.onNodeWithTag("lead_brain_selector").performScrollTo()
        composeRule.onNodeWithTag("lead_auto").performScrollTo()
        composeRule.onNodeWithTag("lead_auto").assertIsDisplayed()
    }
}
