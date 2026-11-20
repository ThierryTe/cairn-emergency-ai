package com.cairn.emergency.feature.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.cairn.emergency.R
import com.cairn.emergency.ai.Engine
import com.cairn.emergency.ui.components.JourneyNavy
import com.cairn.emergency.ui.components.JourneyScaffold
import com.cairn.emergency.ui.home.CairnGlyph
import com.cairn.emergency.ui.home.CairnIcon
import com.cairn.emergency.ui.settings.CairnLanguage
import com.cairn.emergency.ui.sos.EmergencyGuide
import com.cairn.emergency.ui.sos.SosStatus
import com.cairn.emergency.ui.voice.ReadAloud
import com.cairn.emergency.ui.voice.rememberReadAloud
import com.cairn.emergency.ui.voice.rememberSpeechInput

private val AssistantBlue = Color(0xFF125FC5)
private const val MaxQuestionLength = 500

@Composable
internal fun AssistantScreen(
    model: AssistantViewModel,
    lifecycle: Lifecycle,
    language: CairnLanguage,
    readAloud: Boolean,
    simplified: Boolean,
    onSos: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var question by rememberSaveable { mutableStateOf("") }
    var cloudEnabled by rememberSaveable { mutableStateOf(false) }
    var cloudDialog by rememberSaveable { mutableStateOf(false) }
    val (speech, dictate) = rememberSpeechInput(lifecycle, language.tag) { question = it.take(MaxQuestionLength) }
    val voice = rememberReadAloud(lifecycle, language.tag, readAloud && model.turns.isNotEmpty())
    val list = rememberLazyListState()
    val latest = model.turns.lastOrNull()
    LaunchedEffect(latest?.id) {
        if (latest != null) list.scrollToItem(model.turns.lastIndex)
    }
    val canSend = question.isNotBlank() && !speech.active && !model.ranking
    val send = {
        if (canSend) {
            voice?.stop()
            speech.cancel()
            model.ask(question, context, simplified, cloudEnabled)
            question = ""
        }
    }

    JourneyScaffold(
        title = stringResource(R.string.assistant_title),
        onBack = onBack,
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 5.dp)) {
                    SosStatus(speech.status)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = question,
                            onValueChange = { question = it.take(MaxQuestionLength) },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(R.string.assistant_question)) },
                            shape = RoundedCornerShape(21.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { send() }),
                            maxLines = 3,
                        )
                        val sendLabel = stringResource(R.string.assistant_send)
                        IconButton(
                            onClick = send, enabled = canSend,
                            modifier = Modifier.semantics { contentDescription = sendLabel },
                        ) { CairnGlyph(CairnIcon.Next, if (canSend) AssistantBlue else JourneyNavy.copy(alpha = .45f), Modifier.size(22.dp)) }
                        val micLabel = stringResource(if (speech.active) R.string.voice_stop else R.string.voice_start)
                        FilledIconButton(
                            onClick = { voice?.stop(); dictate() },
                            enabled = !model.ranking || speech.active,
                            modifier = Modifier.size(48.dp).semantics { contentDescription = micLabel },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = AssistantBlue, contentColor = Color.White),
                        ) { CairnGlyph(if (speech.active) CairnIcon.Stop else CairnIcon.Microphone, Color.White, Modifier.size(22.dp)) }
                    }
                }
            }
        },
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                EngineBadge(latest?.engine, Modifier.weight(1f))
                TextButton(onClick = { if (cloudEnabled) cloudEnabled = false else cloudDialog = true }) {
                    Text(stringResource(if (cloudEnabled) R.string.assistant_cloud_on else R.string.assistant_cloud_option))
                }
            }
            if (model.ranking) SosStatus(R.string.assistant_ranking)
            if (model.turns.isEmpty()) {
                Text(stringResource(R.string.assistant_intro), Modifier.padding(12.dp), color = JourneyNavy)
            }
            LazyColumn(state = list, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                itemsIndexed(model.turns, key = { _, turn -> turn.id }) { index, turn ->
                    AssistantExchange(turn, simplified, voice.takeIf { index == model.turns.lastIndex }, onSos)
                }
            }
        }
    }
    if (cloudDialog) {
        AlertDialog(
            onDismissRequest = { cloudDialog = false },
            title = { Text(stringResource(R.string.assistant_cloud_option)) },
            text = { Text(stringResource(R.string.assistant_cloud_consent)) },
            confirmButton = {
                TextButton(onClick = { cloudEnabled = true; cloudDialog = false }) { Text(stringResource(R.string.assistant_allow_cloud)) }
            },
            dismissButton = { TextButton(onClick = { cloudDialog = false }) { Text(stringResource(R.string.kit_close)) } },
        )
    }
}

@Composable
private fun EngineBadge(engine: Engine?, modifier: Modifier = Modifier) {
    val label = when (engine) {
        Engine.LOCAL -> R.string.assistant_local
        Engine.CLOUD -> R.string.assistant_cloud
        null -> R.string.sos_guidance
    }
    val tint = when (engine) {
        Engine.LOCAL -> Color(0xFFD7F5E7)
        Engine.CLOUD -> Color(0xFFD6F4F3)
        null -> Color(0xFFE8E8FC)
    }
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        Surface(shape = RoundedCornerShape(10.dp), color = tint) {
            Text(stringResource(label), Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = JourneyNavy, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun AssistantExchange(turn: AssistantTurn, simplified: Boolean, voice: ReadAloud?, onSos: () -> Unit) {
    val guide = EmergencyGuide.entries.firstOrNull { it.topic == turn.match.topic }
    val steps = guide?.let { stringArrayResource(it.stepsFor(simplified)) }
    val ordered = steps?.let { list -> turn.order?.map { list[it] } ?: list.toList() }
    val reply = if (ordered == null) stringResource(R.string.assistant_no_guide) else ordered.joinToString(" ")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Surface(
            modifier = Modifier.align(Alignment.End).widthIn(max = 290.dp),
            shape = RoundedCornerShape(16.dp), color = Color(0xFFD6E7FC),
        ) {
            Text(turn.question, Modifier.padding(12.dp), color = JourneyNavy)
        }
        if (turn.match.urgent) {
            Surface(color = Color(0xFFFFE9D9), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 11.dp, vertical = 5.dp)) {
                    Text(stringResource(R.string.assistant_safety), color = JourneyNavy, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { voice?.stop(); onSos() }) { Text(stringResource(R.string.sos_title)) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Surface(shape = CircleShape, color = JourneyNavy, modifier = Modifier.size(30.dp)) {
                Box(contentAlignment = Alignment.Center) { CairnGlyph(CairnIcon.Mountain, Color.White, Modifier.size(18.dp)) }
            }
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFEDEFF5), modifier = Modifier.weight(1f)) {
                Column(
                    Modifier.padding(11.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (guide == null || ordered == null) {
                        Text(reply, color = JourneyNavy)
                    } else {
                        Text(stringResource(guide.title), color = JourneyNavy, fontWeight = FontWeight.Bold)
                        ordered.forEach { Text(stringResource(R.string.assistant_bullet, it), color = JourneyNavy) }
                    }
                }
            }
        }
        if (guide != null) {
            EngineBadge(turn.engine, Modifier.padding(start = 37.dp))
            Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFE5EDF9), modifier = Modifier.fillMaxWidth().padding(start = 37.dp)) {
                Row(Modifier.padding(9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CairnGlyph(CairnIcon.Book, AssistantBlue, Modifier.size(18.dp))
                    Text(stringResource(guide.source), color = JourneyNavy, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (voice != null) {
            Row(Modifier.padding(start = 37.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { voice.play(reply) }, enabled = voice.ready,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AssistantBlue),
                ) { Text(stringResource(R.string.tts_listen)) }
                TextButton(onClick = voice::stop, enabled = voice.speaking, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.tts_stop))
                }
            }
            SosStatus(voice.message ?: R.string.tts_loading.takeIf { !voice.ready })
        }
    }
}
