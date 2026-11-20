package com.cairn.emergency.ui.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.cairn.emergency.R
import java.util.Locale

internal class ReadAloud(context: Context, languageTag: String) {
    private val main = Handler(Looper.getMainLooper())
    private val language = Locale.forLanguageTag(languageTag)
    private var closed = false
    private var currentUtterance: String? = null
    private var nextUtterance = 0L

    var ready by mutableStateOf(false)
        private set
    var speaking by mutableStateOf(false)
        private set
    var message by mutableStateOf<Int?>(null)
        private set

    private val engine = TextToSpeech(context.applicationContext) { result ->
        main.post { if (!closed) configure(result) }
    }

    private fun configure(result: Int) {
        if (result != TextToSpeech.SUCCESS) {
            message = R.string.tts_unavailable
            return
        }
        val voice = engine.voices.orEmpty()
            .filter { !it.isNetworkConnectionRequired && it.locale.language == language.language }
            .filter { TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty() }
            .sortedWith(compareByDescending<android.speech.tts.Voice> { it.locale == language }
                .thenByDescending { it.quality }.thenBy { it.latency })
            .firstOrNull()
        if (voice == null || engine.setVoice(voice) == TextToSpeech.ERROR) {
            message = R.string.tts_offline_voice_missing
            return
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finished(utteranceId)
            @Deprecated("Android TTS callback")
            override fun onError(utteranceId: String?) = finished(utteranceId, R.string.tts_unavailable)
            override fun onError(utteranceId: String?, errorCode: Int) = finished(utteranceId, R.string.tts_unavailable)
        })
        ready = true
    }

    fun play(text: String) {
        if (!ready || closed || text.isBlank()) return
        val utterance = "cairn-${++nextUtterance}"
        currentUtterance = utterance
        message = null
        speaking = true
        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utterance) == TextToSpeech.ERROR) {
            finished(utterance, R.string.tts_unavailable)
        }
    }

    fun stop() {
        currentUtterance = null
        speaking = false
        engine.stop()
    }

    private fun finished(id: String?, error: Int? = null) {
        main.post {
            if (!closed && id != null && id == currentUtterance) {
                currentUtterance = null
                speaking = false
                message = error
            }
        }
    }

    fun close() {
        closed = true
        stop()
        engine.shutdown()
    }
}

@Composable
internal fun rememberReadAloud(lifecycle: Lifecycle, languageTag: String, enabled: Boolean): ReadAloud? {
    val context = LocalContext.current
    val voice = remember(context, languageTag, enabled) { if (enabled) ReadAloud(context, languageTag) else null }
    DisposableEffect(lifecycle, voice) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) voice?.stop() }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            voice?.close()
        }
    }
    return voice
}
