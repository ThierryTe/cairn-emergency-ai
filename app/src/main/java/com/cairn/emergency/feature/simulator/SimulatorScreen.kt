package com.cairn.emergency.feature.simulator

import androidx.activity.compose.BackHandler
import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.cairn.emergency.R
import com.cairn.emergency.ui.components.JourneyNavy
import com.cairn.emergency.ui.components.JourneyOutline
import com.cairn.emergency.ui.components.JourneyScaffold
import com.cairn.emergency.ui.home.CairnGlyph
import com.cairn.emergency.ui.home.CairnIcon
import com.cairn.emergency.ui.settings.CairnLanguage
import com.cairn.emergency.ui.sos.SosStatus
import com.cairn.emergency.ui.voice.rememberReadAloud

private val SimulatorTeal = Color(0xFF007986)

private enum class ScenarioCard(
    val scenario: Scenario,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val image: Int,
    @ArrayRes val situations: Int,
    @ArrayRes val choices: Int,
    @ArrayRes val lessons: Int,
    @StringRes val source: Int,
) {
    Flood(
        Scenario.Flood, R.string.sim_flood, R.string.sim_flood_description, R.drawable.sim_flood,
        R.array.sim_flood_situations, R.array.sim_flood_choices, R.array.sim_flood_lessons, R.string.guide_flood_source,
    ),
    Power(
        Scenario.Power, R.string.sim_power, R.string.sim_power_description, R.drawable.sim_power,
        R.array.sim_power_situations, R.array.sim_power_choices, R.array.sim_power_lessons, R.string.guide_power_source,
    ),
}

@Composable
internal fun SimulatorScreen(
    model: SimulatorViewModel,
    lifecycle: Lifecycle,
    language: CairnLanguage,
    readAloud: Boolean,
    onBack: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf<ScenarioCard?>(null) }
    val card = selected
    val backToCards = { selected = null }
    BackHandler(enabled = card != null, onBack = backToCards)
    val progress = model.progress
    val run = progress?.let { card?.scenario?.let(it::get) }

    JourneyScaffold(
        title = stringResource(card?.title ?: R.string.sim_title),
        onBack = if (card == null) onBack else backToCards,
    ) {
        key(card, run?.step) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SosStatus(model.errorMessage)
                when {
                    progress == null && model.errorMessage != null -> {
                        OutlinedButton(onClick = model::load) { Text(stringResource(R.string.retry)) }
                    }
                    progress == null -> SosStatus(R.string.sim_loading)
                    card == null -> ScenarioCard.entries.forEach { scenarioCard ->
                        val saved = progress[scenarioCard.scenario]
                        ScenarioPreview(scenarioCard, saved, !model.busy) {
                            if (saved == null) model.start(scenarioCard.scenario)
                            selected = scenarioCard
                        }
                    }
                    run == null -> {
                        if (model.busy) SosStatus(R.string.sim_loading)
                        else OutlinedButton(onClick = { model.start(card.scenario) }) { Text(stringResource(R.string.retry)) }
                    }
                    run.isFinished(card.scenario) -> ScenarioSummary(
                        card, run, !model.busy, { model.restart(card.scenario) }, backToCards,
                    )
                    else -> ScenarioStep(card, run, model, lifecycle, language, readAloud)
                }
            }
        }
    }
}

@Composable
private fun ScenarioPreview(card: ScenarioCard, run: ScenarioProgress?, enabled: Boolean, onStart: () -> Unit) {
    val action = when {
        run == null -> R.string.sim_start
        run.isFinished(card.scenario) -> R.string.sim_results
        else -> R.string.sim_resume
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, JourneyOutline),
        color = Color.White,
        shadowElevation = 2.dp,
    ) {
        Column {
            ScenarioArt(card)
            Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(stringResource(card.title), color = JourneyNavy, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(stringResource(card.description), color = JourneyNavy, style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = onStart,
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SimulatorTeal),
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(stringResource(action), Modifier.padding(horizontal = 18.dp))
                        CairnGlyph(CairnIcon.Next, Color.White, Modifier.align(Alignment.CenterEnd).size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenarioArt(card: ScenarioCard) {
    Image(
        painter = painterResource(card.image),
        contentDescription = null,
        modifier = Modifier.fillMaxWidth().aspectRatio(2.8f),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun ScenarioStep(
    card: ScenarioCard,
    run: ScenarioProgress,
    model: SimulatorViewModel,
    lifecycle: Lifecycle,
    language: CairnLanguage,
    readAloud: Boolean,
) {
    val step = run.step
    val situation = stringArrayResource(card.situations)[step]
    val choices = stringArrayResource(card.choices).slice(step * 3 until (step + 1) * 3)
    val lesson = stringArrayResource(card.lessons)[step]
    val safest = card.scenario.safestChoice(step)
    val chosen = run.chosen
    var selection by rememberSaveable { mutableStateOf<Int?>(null) }
    val voice = rememberReadAloud(lifecycle, language.tag, readAloud)
    val stepLabel = stringResource(R.string.sim_step, step + 1, card.scenario.length)

    Text(stepLabel, color = JourneyNavy, fontWeight = FontWeight.Bold)
    LinearProgressIndicator(
        progress = { run.decisions.length.toFloat() / card.scenario.length },
        modifier = Modifier.fillMaxWidth(), color = SimulatorTeal,
    )
    Text(situation, color = JourneyNavy, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        choices.forEachIndexed { index, choice ->
            val checked = (chosen ?: selection) == index
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (checked) Color(0xFFDAF1EF) else Color.White,
                border = BorderStroke(1.dp, if (checked) SimulatorTeal else JourneyOutline),
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .selectable(checked, enabled = chosen == null && !model.busy, role = Role.RadioButton) { selection = index }
                        .padding(horizontal = 9.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = checked, enabled = chosen == null, onClick = null)
                    Text(choice, Modifier.padding(start = 8.dp), color = JourneyNavy)
                }
            }
        }
    }
    if (chosen != null) {
        Surface(color = if (chosen == safest) Color(0xFFDDF3E8) else Color(0xFFFFEFDB), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    stringResource(if (chosen == safest) R.string.sim_safest else R.string.sim_safer, if (chosen == safest) 1 else 0),
                    color = JourneyNavy, fontWeight = FontWeight.Bold,
                )
                Text(lesson, color = JourneyNavy)
            }
        }
    }
    if (voice != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { voice.play(if (chosen == null) (listOf(stepLabel, situation) + choices).joinToString(". ") else lesson) },
                enabled = voice.ready, modifier = Modifier.heightIn(min = 48.dp),
            ) {
                CairnGlyph(CairnIcon.Speaker, SimulatorTeal, Modifier.size(18.dp))
                Text(stringResource(R.string.tts_listen), Modifier.padding(start = 6.dp))
            }
            TextButton(onClick = voice::stop, enabled = voice.speaking, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.tts_stop))
            }
        }
        SosStatus(voice.message ?: R.string.tts_loading.takeIf { !voice.ready })
    }
    Button(
        onClick = {
            voice?.stop()
            if (chosen != null) model.continueAfterFeedback(card.scenario)
            else selection?.let { model.choose(card.scenario, it) }
        },
        enabled = !model.busy && (chosen != null || selection != null),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SimulatorTeal),
    ) {
        val action = when {
            chosen == null -> R.string.sim_submit
            step + 1 == card.scenario.length -> R.string.sim_results
            else -> R.string.sim_continue
        }
        Text(stringResource(action))
    }
}

@Composable
private fun ScenarioSummary(
    card: ScenarioCard,
    run: ScenarioProgress,
    enabled: Boolean,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    val scenario = card.scenario
    val lessons = stringArrayResource(card.lessons)
    val score = run.score(scenario)
    Text(stringResource(R.string.sim_complete), style = MaterialTheme.typography.headlineSmall, color = JourneyNavy, fontWeight = FontWeight.ExtraBold)
    Text(stringResource(R.string.sim_score, score, scenario.length), color = JourneyNavy, fontWeight = FontWeight.Bold)
    val review = run.decisions.indices.filter { run.decisions[it].digitToInt() != scenario.safestChoice(it) }
    if (review.isEmpty()) {
        Text(stringResource(R.string.sim_all_safe), color = JourneyNavy)
    } else {
        Text(stringResource(R.string.sim_review), color = JourneyNavy, fontWeight = FontWeight.Bold)
        review.forEach { index ->
            Text(stringResource(R.string.guide_step, index + 1, lessons[index]), color = JourneyNavy)
        }
    }
    Text(stringResource(card.source), color = JourneyNavy, style = MaterialTheme.typography.labelMedium)
    OutlinedButton(onClick = onRestart, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(stringResource(R.string.sim_restart))
    }
    TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(stringResource(R.string.sim_back))
    }
}
