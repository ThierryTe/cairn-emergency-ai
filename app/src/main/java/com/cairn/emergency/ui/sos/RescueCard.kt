package com.cairn.emergency.ui.sos

import android.content.Context
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cairn.emergency.R
import com.cairn.emergency.data.HouseholdProfile
import com.cairn.emergency.ui.components.JourneyNavy
import com.cairn.emergency.ui.components.JourneyPurple
import com.cairn.emergency.ui.home.CairnIcon
import com.cairn.emergency.ui.voice.ReadAloud

internal fun Context.formatRescueCard(draft: SosDraft): String = listOf(
    R.string.rescue_incident to draft.incident,
    R.string.rescue_people to draft.people,
    R.string.rescue_needs to draft.urgentNeeds,
    R.string.voice_sos_description to draft.notes,
    R.string.rescue_location to draft.manualLocation,
).filter { it.second.isNotBlank() }.joinToString("\n") { (label, value) ->
    getString(R.string.rescue_field_line, getString(label), value.trim())
}

@Composable
internal fun RescueCard(
    profile: HouseholdProfile?,
    draft: SosDraft,
    onDraftChange: (SosDraft) -> Unit,
    location: SosLocationAction,
    voice: ReadAloud?,
    onRequest: (SosCommunication) -> Unit,
    prepareSms: Boolean = false,
) {
    val context = LocalContext.current
    var fullScreen by remember { mutableStateOf(false) }
    var smsOpen by remember { mutableStateOf(prepareSms) }
    var smsOverride by remember { mutableStateOf<String?>(null) }
    val attached = draft.deviceLocation?.takeIf { it.isFresh(SystemClock.elapsedRealtimeNanos()) }
    val stale = draft.deviceLocation != null && attached == null
    val cardBody = context.formatRescueCard(draft)
    val cardText = context.appendSosLocation(cardBody, attached)

    fun update(next: SosDraft) {
        onDraftChange(next)
        if (next.copy(deviceLocation = draft.deviceLocation) != draft) smsOverride = null
    }

    Text(stringResource(R.string.rescue_intro), color = JourneyNavy)
    RescueField(R.string.rescue_incident, draft.incident) { update(draft.copy(incident = it)) }
    RescueField(R.string.rescue_people, draft.people) { update(draft.copy(people = it)) }
    val householdCount = profile?.let { listOfNotNull(it.adults, it.children, it.infants) }
        ?.takeIf { it.size == 3 && it.all { count -> count >= 0 } }?.sum()?.takeIf { it in 1..999 }
    if (householdCount != null) {
        val householdPeople = pluralStringResource(R.plurals.kit_people, householdCount, householdCount)
        TextButton(
            onClick = { update(draft.copy(people = householdPeople)) },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.voice_sos_household_count, householdPeople)) }
    }
    RescueField(R.string.rescue_needs, draft.urgentNeeds, limit = 460) { update(draft.copy(urgentNeeds = it)) }
    val savedNeeds = listOfNotNull(profile?.medicalNeeds, profile?.accessibility).filter(String::isNotBlank).distinct().joinToString("\n")
    if (savedNeeds.isNotBlank()) {
        TextButton(
            onClick = { update(draft.copy(urgentNeeds = listOf(draft.urgentNeeds, savedNeeds).filter(String::isNotBlank).distinct().joinToString("\n").take(460))) },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.voice_sos_household_needs)) }
    }
    RescueField(R.string.voice_sos_description, draft.notes, limit = MaxSosDescription) { update(draft.copy(notes = it)) }
    RescueField(R.string.rescue_location, draft.manualLocation) { update(draft.copy(manualLocation = it)) }

    SosLocationPanel(location, attached, onUse = { update(draft.copy(deviceLocation = it)) })
    if (stale) SosStatus(R.string.voice_sos_location_stale)

    OutlinedButton(
        onClick = { fullScreen = true },
        enabled = cardText.isNotBlank(),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) { Text(stringResource(R.string.rescue_show_fullscreen)) }
    if (cardText.isNotBlank()) {
        SosAudioButtons(voice, R.string.voice_sos_read_card, cardText)
        TextButton(
            onClick = { onRequest(SosCommunication.CopyCard(cardBody, attached)) },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.rescue_copy)) }
    }

    Button(
        onClick = {
            smsOpen = true
            smsOverride = null
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) { Text(stringResource(R.string.sos_prepare_sms)) }
    if (smsOpen) {
        val defaultBody = if (draft.hasContent) {
            listOf(context.getString(R.string.voice_sos_sms_header), cardBody)
                .filter(String::isNotBlank).joinToString("\n\n")
        } else {
            context.getString(R.string.sos_draft_template)
        }
        val body = smsOverride ?: defaultBody
        RescueField(R.string.sos_draft_label, body, limit = 2500) { smsOverride = it }
        if (attached != null) {
            Text(stringResource(R.string.sos_location_line, context.formatSosLocation(attached)), color = JourneyNavy)
        }
        Button(
            onClick = { onRequest(SosCommunication.Messages(body, attached)) },
            enabled = body.isNotBlank(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.sos_open_messages)) }
    }

    if (fullScreen && cardText.isNotBlank()) {
        Dialog(onDismissRequest = { fullScreen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                Column(
                    Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    Text(stringResource(R.string.sos_rescue), color = JourneyNavy, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    SelectionContainer { Text(cardText, color = JourneyNavy, style = MaterialTheme.typography.titleLarge) }
                    SosAudioButtons(voice, R.string.voice_sos_read_card, cardText)
                    Button(
                        onClick = { fullScreen = false },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.kit_close)) }
                }
            }
        }
    }
}

@Composable
internal fun SosLocationPanel(
    location: SosLocationAction,
    attached: SosLocationFix?,
    onUse: (SosLocationFix?) -> Unit,
) {
    val context = LocalContext.current
    SosCard(
        R.string.rescue_add_location, R.string.voice_sos_location_preview, CairnIcon.Next, JourneyPurple,
        enabled = !location.busy,
        onClick = { location.request(true) },
    )
    SosStatus(location.status)
    val fix = location.fix
    if (fix != null) {
        SelectionContainer { Text(context.formatSosLocation(fix), color = JourneyNavy) }
        if (fix != attached) {
            TextButton(
                onClick = { if (fix.isFresh(SystemClock.elapsedRealtimeNanos())) onUse(fix) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.voice_sos_location_use)) }
        }
    }
    if (attached != null) {
        Text(stringResource(R.string.voice_sos_location_attached, context.formatSosLocation(attached)), color = JourneyNavy)
        TextButton(onClick = { onUse(null) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.voice_sos_location_remove))
        }
    }
}

@Composable
internal fun SosAudioButtons(voice: ReadAloud?, @StringRes label: Int, body: String) {
    if (voice == null || body.isBlank()) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { voice.play(body) },
            enabled = voice.ready,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        ) { Text(stringResource(label)) }
        TextButton(
            onClick = voice::stop,
            enabled = voice.speaking,
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.tts_stop)) }
    }
    SosStatus(voice.message ?: R.string.tts_loading.takeIf { !voice.ready })
}

@Composable
internal fun RescueField(@StringRes label: Int, value: String, limit: Int = 280, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(limit)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        maxLines = 6,
    )
}
