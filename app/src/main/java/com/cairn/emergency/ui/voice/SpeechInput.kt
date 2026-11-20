package com.cairn.emergency.ui.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.cairn.emergency.R
import java.util.Locale
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal enum class SpeechMode { PreferDevice, DeviceOnly, System }

internal class SpeechInput(private val context: Context, private val onTranscript: (String) -> Unit) {
    private var recognizer: SpeechRecognizer? = null
    private val scope = MainScope()
    private var timeout: Job? = null
    var active by mutableStateOf(false)
        private set
    var status by mutableStateOf<Int?>(null)
        private set

    fun supportsDeviceRecognition() =
        Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    fun start(languageTag: String, mode: SpeechMode) {
        val local = mode != SpeechMode.System && supportsDeviceRecognition()
        if (!local && mode == SpeechMode.DeviceOnly) {
            status = R.string.voice_sos_local_unavailable
            return
        }
        if (!local && !SpeechRecognizer.isRecognitionAvailable(context)) {
            status = R.string.voice_unavailable
            return
        }
        cancel()
        active = true
        status = if (local) R.string.voice_starting else R.string.voice_network_notice
        try {
            val current = if (local && Build.VERSION.SDK_INT >= 31) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
            recognizer = current
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            current.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (recognizer === current) status = if (local) R.string.voice_listening else R.string.voice_network_notice
                }
                override fun onResults(results: Bundle?) {
                    if (recognizer !== current) return
                    val transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim()
                    cancel(if (transcript.isNullOrEmpty()) R.string.voice_no_match else R.string.voice_review)
                    if (!transcript.isNullOrEmpty()) onTranscript(transcript)
                }
                override fun onError(error: Int) {
                    if (recognizer === current) cancel(errorMessage(error))
                }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            timeout = scope.launch {
                delay(35_000)
                if (recognizer === current) cancel(R.string.voice_no_match)
            }
            if (Build.VERSION.SDK_INT >= 33) {
                current.checkRecognitionSupport(intent, ContextCompat.getMainExecutor(context), object : RecognitionSupportCallback {
                    override fun onSupportResult(support: RecognitionSupport) {
                        if (recognizer !== current) return
                        val installed = support.installedOnDeviceLanguages
                        val online = support.onlineLanguages
                        val known = installed + support.supportedOnDeviceLanguages + support.pendingOnDeviceLanguages + online
                        val available = if (local) installed else installed + online
                        val language = Locale.forLanguageTag(languageTag).language
                        val matching = available.firstOrNull { it.equals(languageTag, ignoreCase = true) }
                            ?: available.firstOrNull { Locale.forLanguageTag(it).language == language }
                        when {
                            matching != null -> begin(current, intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, matching))
                            known.isEmpty() -> begin(current, intent)
                            else -> cancel(R.string.voice_language_unavailable)
                        }
                    }
                    override fun onError(error: Int) {
                        if (recognizer !== current) return
                        if (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) {
                            cancel(R.string.voice_language_unavailable)
                        } else {
                            begin(current, intent)
                        }
                    }
                })
            } else {
                begin(current, intent)
            }
        } catch (_: RuntimeException) {
            cancel(R.string.voice_unavailable)
        }
    }

    private fun begin(current: SpeechRecognizer, intent: Intent) {
        try {
            current.startListening(intent)
        } catch (_: RuntimeException) {
            cancel(R.string.voice_unavailable)
        }
    }

    private fun errorMessage(error: Int) = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.voice_denied
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> R.string.voice_language_unavailable
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> R.string.voice_network_error
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> R.string.voice_no_match
        else -> R.string.voice_unavailable
    }

    fun cancel(@StringRes message: Int? = null) {
        timeout?.cancel()
        timeout = null
        val previous = recognizer
        recognizer = null
        previous?.destroy()
        active = false
        status = message
    }

    fun close() {
        cancel()
        scope.cancel()
    }
}

@Composable
internal fun rememberSpeechInput(
    lifecycle: Lifecycle,
    languageTag: String,
    mode: SpeechMode = SpeechMode.PreferDevice,
    onTranscript: (String) -> Unit,
): Pair<SpeechInput, () -> Unit> {
    val context = LocalContext.current
    val transcript by rememberUpdatedState(onTranscript)
    val input = remember(context, languageTag) { SpeechInput(context) { transcript(it) } }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) input.cancel(R.string.voice_denied)
        else if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) input.start(languageTag, mode)
    }
    DisposableEffect(lifecycle, input) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && input.active) input.cancel(R.string.voice_cancelled)
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            input.close()
        }
    }

    return input to {
        when {
            input.active -> input.cancel(R.string.voice_cancelled)
            mode == SpeechMode.DeviceOnly && !input.supportsDeviceRecognition() -> input.cancel(R.string.voice_sos_local_unavailable)
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> input.start(languageTag, mode)
            else -> launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
