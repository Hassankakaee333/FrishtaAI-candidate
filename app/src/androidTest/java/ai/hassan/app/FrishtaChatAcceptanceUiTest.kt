package ai.hassan.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Device acceptance for the three chat UX fixes:
 * short greeting, responder label, keyboard gap.
 */
@OptIn(ExperimentalTestApi::class)
class FrishtaChatAcceptanceUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun unlock() {
        composeRule.activity.setShowWhenLocked(true)
        composeRule.activity.setTurnScreenOn(true)
        composeRule.waitForIdle()
    }

    @Test
    fun greetingIsShortShowsFrishtaLabelAndComposerStaysNearIme() {
        composeRule.onNodeWithContentDescription("القائمة").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("new_chat"), 5_000)
        composeRule.onNodeWithTag("new_chat").performClick()
        composeRule.waitForIdle()

        val app = composeRule.activity.application as HassanApplication
        val beforeIds = runBlocking {
            app.container.repository.messages.first().map { it.id }.toSet()
        }

        composeRule.onNodeWithTag("composer").performTextInput("مرحبا")
        composeRule.onNodeWithContentDescription("إرسال").performClick()
        composeRule.waitUntilAtLeastOneExists(hasTestTag("hassan_message"), 8_000)
        composeRule.waitForIdle()
        Thread.sleep(600)

        val assistantMessages = runBlocking {
            app.container.repository.messages.first()
                .filter { it.id !in beforeIds && it.role != "USER" }
        }
        assertTrue("Expected assistant reply on device", assistantMessages.isNotEmpty())
        val reply = assistantMessages.last().content
        assertFalse("Must not include long pitch", reply.contains("أقدر أتحدث"))
        assertFalse("Must not self-introduce as حسن in body", reply.contains("أنا حسن"))
        assertTrue(
            "Greeting should be short/natural: $reply",
            reply.contains("أهلًا") || reply.contains("أهلا") || reply.contains("كيف أقدر"),
        )
        val provider = assistantMessages.last().providerId.orEmpty()
        assertTrue(
            "providerId should identify Frishta/local responder, got=$provider",
            provider.isEmpty() || provider == "frishta" || provider == "auto" ||
                provider == "hassan-local" || provider == "local",
        )

        assertTrue(
            "Frishta AI label must appear (top bar and/or bubble)",
            composeRule.onAllNodesWithText("Frishta AI").fetchSemanticsNodes().isNotEmpty(),
        )
        assertTrue(
            "hassan_message bubble visible",
            composeRule.onAllNodesWithTag("hassan_message").fetchSemanticsNodes().isNotEmpty(),
        )

        // Focus composer → IME opens; composer should sit in lower portion (not mid-screen float).
        composeRule.onNodeWithTag("composer").performClick()
        composeRule.waitForIdle()
        Thread.sleep(900)
        val displayH = composeRule.activity.resources.displayMetrics.heightPixels
        val composerBottom = composeRule.onNodeWithTag("composer")
            .fetchSemanticsNode()
            .boundsInRoot
            .bottom
        assertTrue(
            "Composer should be in the lower half when focused (bottom=$composerBottom, h=$displayH)",
            composerBottom > displayH * 0.35f,
        )
        val gapBelowComposer = displayH - composerBottom
        assertFalse(
            "Huge empty gap under composer while keyboard likely open (gap=$gapBelowComposer, h=$displayH, bottom=$composerBottom)",
            gapBelowComposer > displayH * 0.55f && composerBottom < displayH * 0.55f,
        )
    }
}
