package com.cairn.emergency.ui.sos

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cairn.emergency.R
import com.cairn.emergency.feature.assistant.GuideTopic
import com.cairn.emergency.ui.components.JourneyNavy
import com.cairn.emergency.ui.components.JourneyPurple
import com.cairn.emergency.ui.home.CairnIcon
import com.cairn.emergency.ui.voice.ReadAloud

internal enum class EmergencyGuide(
    val topic: GuideTopic,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @ArrayRes val steps: Int,
    @ArrayRes val simpleSteps: Int,
    @StringRes val source: Int,
    val icon: CairnIcon,
) {
    Flood(GuideTopic.Flood, R.string.guide_flood, R.string.guide_flood_desc, R.array.guide_flood_steps, R.array.guide_flood_simple, R.string.guide_flood_source, CairnIcon.Water),
    Power(GuideTopic.Power, R.string.guide_power, R.string.guide_power_desc, R.array.guide_power_steps, R.array.guide_power_simple, R.string.guide_power_source, CairnIcon.Simulator),
    Fire(GuideTopic.Fire, R.string.guide_fire, R.string.guide_fire_desc, R.array.guide_fire_steps, R.array.guide_fire_simple, R.string.guide_fire_source, CairnIcon.Siren),
    Earthquake(GuideTopic.Earthquake, R.string.guide_quake, R.string.guide_quake_desc, R.array.guide_quake_steps, R.array.guide_quake_simple, R.string.guide_quake_source, CairnIcon.Mountain),
    Insulin(GuideTopic.InsulinStorage, R.string.guide_insulin, R.string.guide_insulin_desc, R.array.guide_insulin_steps, R.array.guide_insulin_simple, R.string.guide_insulin_source, CairnIcon.Medicine);

    @ArrayRes fun stepsFor(simplified: Boolean) = if (simplified) simpleSteps else steps
}

@Composable
internal fun OfflineGuidance(
    voice: ReadAloud?,
    simplified: Boolean,
    initialGuide: EmergencyGuide?,
    readOnOpen: Boolean,
) {
    var selected by rememberSaveable(initialGuide) { mutableStateOf(initialGuide) }
    var autoRead by remember { mutableStateOf(readOnOpen && initialGuide != null) }

    SosContent {
        Text(stringResource(R.string.sos_guidance_intro), color = JourneyNavy)
        EmergencyGuide.entries.forEach { guide ->
            SosCard(guide.title, guide.description, guide.icon, JourneyPurple) {
                voice?.stop()
                autoRead = false
                selected = guide.takeUnless { selected == it }
            }
            if (guide == selected) {
                val steps = stringArrayResource(guide.stepsFor(simplified))
                val spoken = stringResource(guide.title) + ". " + steps.joinToString(" ")
                LaunchedEffect(autoRead, voice?.ready) {
                    if (autoRead && voice?.ready == true) {
                        autoRead = false
                        voice.play(spoken)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFE8EFFA),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        steps.forEachIndexed { index, step ->
                            Text(stringResource(R.string.guide_step, index + 1, step), color = JourneyNavy)
                        }
                        Text(
                            stringResource(guide.source),
                            color = JourneyNavy,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (voice != null) SosAudioButtons(voice, R.string.tts_listen, spoken)
                        else SosStatus(R.string.tts_disabled)
                    }
                }
            }
        }
        SosDisclaimer()
    }
}
