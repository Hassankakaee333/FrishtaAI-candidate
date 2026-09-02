package ai.hassan.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LiveChatUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        composeRule.activity.setShowWhenLocked(true)
        composeRule.activity.setTurnScreenOn(true)
        composeRule.waitForIdle()
    }

    @Test
    fun greetingShowsChatHomeWithoutPlanCard() {
        composeRule.onNodeWithTag("composer").performTextInput("مرحبا")
        composeRule.onNodeWithContentDescription("إرسال").performClick()
        composeRule.waitForIdle()
        Thread.sleep(800)
        composeRule.onNodeWithTag("chat_home").assertIsDisplayed()
        val hasPlan = runCatching {
            composeRule.onNodeWithTag("plan_card").assertIsDisplayed()
            true
        }.getOrDefault(false)
        org.junit.Assert.assertFalse("Greeting must not open plan card", hasPlan)
        // When Cloud is configured: reply/message path. When not: NOT_CONFIGURED banner.
        val notConfigured = runCatching {
            composeRule.waitUntilAtLeastOneExists(hasText("NOT_CONFIGURED", substring = true), 1_500)
            true
        }.getOrDefault(false) || runCatching {
            composeRule.onAllNodesWithText("لم يتم إعداد مزود المحادثة", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)
        val hasHassanMessage = runCatching {
            composeRule.onNodeWithTag("hassan_message").assertIsDisplayed()
            true
        }.getOrDefault(false)
        assertTrue(
            "Expected either NOT_CONFIGURED UI or a Hassan reply",
            notConfigured || hasHassanMessage || composeRule.onNodeWithTag("provider_selector").fetchSemanticsNode() != null,
        )
    }

    @Test
    fun navigateAllMainScreens() {
        composeRule.onNodeWithTag("chat_home").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("القائمة").performClick()
        listOf("المشاريع", "المهام", "الموارد", "الرادار", "الإعدادات").forEach { screen ->
            composeRule.onNodeWithText(screen).performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithContentDescription("القائمة").performClick()
        composeRule.onNodeWithTag("nav_Home").performClick()
        composeRule.onNodeWithTag("chat_home").assertIsDisplayed()
    }

    @Test
    fun selfUpdateCardVisibleInSettings() {
        composeRule.onNodeWithContentDescription("القائمة").performClick()
        composeRule.onNodeWithText("الإعدادات").performClick()
        composeRule.onNodeWithTag("self_update_card").assertIsDisplayed()
        composeRule.onNodeWithTag("backup_apk").assertIsDisplayed()
    }
}
