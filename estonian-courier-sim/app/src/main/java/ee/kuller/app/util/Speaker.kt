package ee.kuller.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Озвучка эстонских слов через системный TextToSpeech.
 * Эстонская локаль ("et") поддерживается не на всех устройствах —
 * если её нет, движок просто промолчит, приложение продолжит работать.
 */
class Speaker(context: Context) {
    private var ready = false
    private val tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) ready = true
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        val estonian = Locale("et", "EE")
        val res = tts.setLanguage(estonian)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.setLanguage(Locale.getDefault())
        }
        tts.setSpeechRate(0.9f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

/** Composable-обёртка: создаёт Speaker и корректно освобождает его. */
@Composable
fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    val speaker = remember { Speaker(context) }
    DisposableEffect(Unit) {
        onDispose { speaker.shutdown() }
    }
    return speaker
}
