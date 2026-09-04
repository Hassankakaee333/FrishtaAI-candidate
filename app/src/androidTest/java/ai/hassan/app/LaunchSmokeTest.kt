package ai.hassan.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LaunchSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun makeTestActivityVisibleOnLockedDevice() {
        composeRule.activity.setShowWhenLocked(true)
        composeRule.activity.setTurnScreenOn(true)
        composeRule.waitForIdle()
    }

    @Test
    fun chatFirstHomeExposesComposerAndProviderIndicator() {
        composeRule.onNodeWithTag("chat_home").assertIsDisplayed()
        composeRule.onNodeWithTag("composer").assertIsDisplayed()
        composeRule.onNodeWithTag("provider_status_dot").assertIsDisplayed()
    }

    @Test
    fun codexReasoningEffortCanBeSelectedFromSettingsAndPersists() {
        val application = composeRule.activity.application as HassanApplication
        val repository = application.container.repository

        runBlocking {
            val conversation = InstrumentedTestSupport.isolatedConversation(repository, "codex-persist")
            repository.selectLeadBrain("chatgpt")
            repository.selectCodexReasoningEffort("high")
            val saved = repository.conversations.first().firstOrNull { it.id == conversation.id }
            assertEquals("high", saved?.codexReasoningEffort)
        }

        composeRule.onNodeWithTag("new_chat").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("القائمة").performClick()
        composeRule.onNodeWithText("الإعدادات").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("settings_scroll"), 8_000)
        composeRule.onNodeWithTag("settings_scroll").performScrollToNode(hasTestTag("lead_brain_selector"))
        composeRule.onNodeWithTag("lead_chatgpt").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("codex_effort_selector"), 8_000)
        composeRule.onNodeWithTag("codex_usage_card").assertExists()
        composeRule.onNodeWithText("لا توجد قراءة بعد").assertExists()
        composeRule.onNodeWithTag("effort_high").assertExists()
        composeRule.onNodeWithText("عالٍ").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun planApprovalMovesToHumanGatedBridgeDeterministically() {
        val application = composeRule.activity.application as HassanApplication
        runBlocking { application.container.repository.selectLeadBrain("chatgpt") }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("composer").performTextInput("ابن ميزة اختبار صغيرة وآمنة")
        composeRule.onNodeWithContentDescription("إرسال").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("الخطة جاهزة"), 8_000)
        composeRule.onNodeWithTag("conversation_list").performScrollToNode(hasTestTag("plan_card"))
        composeRule.onNodeWithTag("plan_card").assertIsDisplayed()

        composeRule.onNodeWithTag("composer").performTextInput("ابدأ")
        composeRule.onNodeWithContentDescription("إرسال").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("bridge_send"), 8_000)
        composeRule.onNodeWithTag("conversation_list").performScrollToNode(hasTestTag("bridge_send"))
        composeRule.onNodeWithTag("bridge_send").assertIsDisplayed()
    }

    @Test
    fun diagnosticsShowsDeviceSecurityAndZeroCostFields() {
        composeRule.onNodeWithContentDescription("القائمة").performClick()
        composeRule.onNodeWithText("Diagnostics").performClick()

        composeRule.onNodeWithTag("diagnostics_list").performScrollToNode(hasText("Free-only status"))
        composeRule.onNodeWithText("ACTIVE / immutable").assertIsDisplayed()
        composeRule.onNodeWithTag("diagnostics_list").performScrollToNode(hasText("StrongBox"))
        composeRule.onNodeWithText("StrongBox").assertIsDisplayed()
    }
}
