package ai.hassan.app.conversation

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Speaks assistant replies in Arabic when voice conversation is active. */
class HassanTts(context: Context) : TextToSpeech.OnInitListener {
    private var ready = false
    private val tts = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            val ar = Locale("ar")
            val result = tts.setLanguage(ar)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.language = Locale.US
            }
        }
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        val cleaned = text
            .replace(Regex("[#*_>`\\[\\]()]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(600)
        if (cleaned.isBlank()) return
        tts.stop()
        tts.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, "frishta-reply")
    }

    fun stop() {
        runCatching { tts.stop() }
    }

    fun shutdown() {
        runCatching {
            tts.stop()
            tts.shutdown()
        }
        ready = false
    }
}
