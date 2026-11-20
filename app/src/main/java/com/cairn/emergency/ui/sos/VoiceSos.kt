package com.cairn.emergency.ui.sos

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.cairn.emergency.R
import com.cairn.emergency.data.HouseholdProfile
import com.cairn.emergency.feature.assistant.matchGuide
import com.cairn.emergency.ui.components.JourneyNavy
import com.cairn.emergency.ui.home.CairnGlyph
import com.cairn.emergency.ui.home.CairnIcon
import com.cairn.emergency.ui.settings.CairnLanguage
import com.cairn.emergency.ui.voice.ReadAloud
import com.cairn.emergency.ui.voice.SpeechMode
import com.cairn.emergency.ui.voice.rememberSpeechInput

@Composable
internal fun VoiceSos(
    profile: HouseholdProfile?,
    draft: SosDraft,
    onDraftChange: (SosDraft) -> Unit,
    language: CairnLanguage,
    lifecycle: Lifecycle,
    location: SosLocationAction,
    voice: ReadAloud?,
    autoListen: Boolean,
    onAutoListenHandled: () -> Unit,
    onRequest: (SosCommunication) -> Unit,
    onOpenSos: () -> Unit,
    onHelp: () -> Unit,
    onRescue: () -> Unit,
    onGuidance: (EmergencyGuide?, Boolean) -> Unit,
) {
    val context = LocalContext.current
    var transcript by remember { mutableStateOf("") }
    var useSystemSpeech by remember { mutableStateOf(false) }
    var systemConsent by remember { mutableStateOf(false) }
    var startSystemSpeech by remember { mutableStateOf(false) }
    var speakPosition by remember { mutableStateOf(false) }
    var showDraft by remember { mutableStateOf(draft.hasContent) }
    var prepareMessage by remember { mutableStateOf(false) }
    var draftRevision by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf<Int?>(null) }
    val mode = if (useSystemSpeech) SpeechMode.System else SpeechMode.DeviceOnly
    val (speech, toggleSpeech) = rememberSpeechInput(lifecycle, language.tag, mode) { transcript = it.take(MaxSosDescription) }

    LaunchedEffect(autoListen, startSystemSpeech) {
        if (autoListen || startSystemSpeech) {
            onAutoListenHandled()
            startSystemSpeech = false
            voice?.stop()
            toggleSpeech()
        }
    }
    LaunchedEffect(speakPosition, location.busy, location.fix, location.status, voice?.ready, voice?.message) {
        if (!speakPosition || location.busy) return@LaunchedEffect
        val fix = location.fix
        if (voice?.ready == true && fix != null) {
            speakPosition = false
            voice.play(context.formatSosLocation(fix))
        } else if (voice?.ready == true && location.status != null) {
            speakPosition = false
            voice.play(context.getString(location.status))
        } else if (voice == null || voice.message != null) {
            speakPosition = false
        }
    }

    fun showPreparedDraft(message: Boolean) {
        showDraft = true
        prepareMessage = message
        draftRevision++
        status = R.string.voice_sos_review
        voice?.play(context.getString(R.string.voice_sos_review))
    }

    fun interpret() {
        if (transcript.isBlank() || speech.active) return
        speakPosition = false
        voice?.stop()
        speech.cancel()
        status = null
        when (val command = parseSosCommand(transcript)) {
            SosCommand.OpenSos -> onOpenSos()
            SosCommand.Help -> onHelp()
            SosCommand.Dialer -> onRequest(SosCommunication.Dialer)
            SosCommand.RescueCard -> onRescue()
            SosCommand.Message -> {
                showPreparedDraft(message = true)
                location.request(false)
            }
            SosCommand.Locate, SosCommand.ReadLocation -> {
                speakPosition = command == SosCommand.ReadLocation
                if (location.fix == null) location.request(true)
            }
            SosCommand.Guidance, SosCommand.ReadGuidance -> {
                val topic = matchGuide(transcript).topic ?: matchGuide(draft.incident).topic
                onGuidance(EmergencyGuide.entries.firstOrNull { it.topic == topic }, command == SosCommand.ReadGuidance)
            }
            SosCommand.StopReading -> status = R.string.voice_sos_stopped
            SosCommand.Describe -> {
                val extracted = interpretEmergency(transcript)
                onDraftChange(
                    SosDraft(
                        incident = extracted.incident?.let { context.getString(it.label) }.orEmpty(),
                        people = extracted.people?.toString().orEmpty(),
                        urgentNeeds = extracted.urgentNeeds,
                        notes = transcript.trim(),
                    ),
                )
                location.request(false)
                showPreparedDraft(message = true)
            }
        }
    }

    SosContent {
        Text(stringResource(R.string.voice_sos_intro), color = JourneyNavy)
        Button(
            onClick = {
                speakPosition = false
                status = null
                voice?.stop()
                toggleSpeech()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SosOrange, contentColor = SosInk),
        ) {
            CairnGlyph(if (speech.active) CairnIcon.Stop else CairnIcon.Microphone, SosInk, Modifier.size(23.dp))
            Text(stringResource(if (speech.active) R.string.voice_stop else R.string.voice_sos_talk), Modifier.padding(start = 9.dp))
        }
        SosStatus(speech.status)
        if (!useSystemSpeech && !speech.active && speech.status in setOf(
                R.string.voice_sos_local_unavailable, R.string.voice_language_unavailable, R.string.voice_unavailable,
                R.string.voice_network_error,
            )
        ) {
            TextButton(onClick = { systemConsent = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.voice_sos_system))
            }
        }
        if (useSystemSpeech) SosStatus(R.string.voice_sos_system_active)
        RescueField(R.string.voice_sos_transcript, transcript, limit = MaxSosDescription) { transcript = it }
        OutlinedButton(
            onClick = ::interpret,
            enabled = transcript.isNotBlank() && !speech.active,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.voice_sos_understand)) }
        SosStatus(status)
        if (showDraft) {
            Text(stringResource(R.string.sos_rescue), style = MaterialTheme.typography.titleLarge, color = JourneyNavy, fontWeight = FontWeight.Bold)
            key(draftRevision) {
                RescueCard(profile, draft, onDraftChange, location, voice, onRequest, prepareSms = prepareMessage)
            }
        } else {
            SosLocationPanel(location, attached = null) { fix ->
                onDraftChange(draft.copy(deviceLocation = fix))
                showDraft = true
            }
        }
        if (voice?.speaking == true) {
            TextButton(onClick = voice::stop, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.tts_stop))
            }
        }
        SosDisclaimer()
    }
    if (systemConsent) {
        AlertDialog(
            onDismissRequest = { systemConsent = false },
            title = { Text(stringResource(R.string.voice_sos_system)) },
            text = { Text(stringResource(R.string.voice_sos_system_detail)) },
            confirmButton = {
                TextButton(onClick = {
                    systemConsent = false
                    useSystemSpeech = true
                    startSystemSpeech = true
                }) { Text(stringResource(R.string.voice_sos_system_allow)) }
            },
            dismissButton = {
                TextButton(onClick = { systemConsent = false }) { Text(stringResource(R.string.household_cancel)) }
            },
        )
    }
}

private val SosIncident.label: Int
    get() = when (this) {
        SosIncident.Flood -> R.string.guide_flood
        SosIncident.HomeFire -> R.string.guide_fire
        SosIncident.PowerOutage -> R.string.guide_power
        SosIncident.Earthquake -> R.string.guide_quake
        SosIncident.Injury -> R.string.voice_sos_reported_injury
    }
