package ai.hassan.app

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttachmentPickerInstrumentedTest {
    private val intentsRule = IntentsRule()
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(intentsRule).around(composeRule)

    @Test
    fun attachmentButtonLaunchesDocumentPickerWithoutCrashing() {
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWith(ActivityResult(Activity.RESULT_CANCELED, null))

        composeRule.onNodeWithTag("attach_button").assertIsDisplayed().performClick()

        intended(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        composeRule.onNodeWithTag("chat_home").assertIsDisplayed()
    }
}
