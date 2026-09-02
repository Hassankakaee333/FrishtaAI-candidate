package ai.hassan.app.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalHassanChatTest {
    @Test
    fun greetingIsNaturalArabic() {
        val reply = LocalHassanChat.reply("مرحبا")
        assertTrue(reply.contains("أهلًا") || reply.contains("اهلا") || reply.contains("أهلا"))
        assertTrue(reply.contains("Frishta AI"))
        assertFalse(reply.contains("أقدر أتحدث"))
        assertFalse(reply.contains("أنا حسن"))
        assertFalse(reply.contains("NOT_CONFIGURED"))
        assertFalse(reply.contains("Hassan Cloud يعمل"))
    }

    @Test
    fun howAreYouGetsConversationReply() {
        val reply = LocalHassanChat.reply("كيف حالك؟")
        assertTrue(reply.contains("بخير") || reply.contains("أساعدك"))
    }

    @Test
    fun geminiIdentifiesAsGeminiInsideFrishta() {
        val reply = LocalHassanChat.reply("ما اسمك؟", providerId = "gemini")
        assertTrue(reply.contains("Gemini"))
        assertTrue(reply.contains("Frishta AI"))
        assertFalse(reply.contains("ChatGPT"))
    }

    @Test
    fun chatgptIdentifiesAsChatGPTInsideFrishta() {
        val reply = LocalHassanChat.reply("من أنت؟", providerId = "chatgpt")
        assertEquals("أنا ChatGPT داخل تطبيق Frishta AI.", reply.trim())
    }

    @Test
    fun capabilitiesListsSelfImprove() {
        val reply = LocalHassanChat.reply("ما إمكانياتك؟", providerId = "gemini")
        assertTrue(reply.contains("حسّن التطبيق") || reply.contains("تحسين التطبيق"))
        assertTrue(reply.contains("الطقس"))
        assertTrue(reply.contains("ارجع للنسخة السابقة"))
    }
}
