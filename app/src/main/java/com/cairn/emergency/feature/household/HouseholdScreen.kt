package com.cairn.emergency.feature.household

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.cairn.emergency.R
import com.cairn.emergency.data.HouseholdProfile
import com.cairn.emergency.ui.components.JourneyNavy
import com.cairn.emergency.ui.components.JourneyOutline
import com.cairn.emergency.ui.components.JourneyPaper
import com.cairn.emergency.ui.components.JourneyPurple
import com.cairn.emergency.ui.components.JourneyScaffold
import com.cairn.emergency.ui.components.JourneyTeal
import com.cairn.emergency.ui.home.CairnGlyph
import com.cairn.emergency.ui.home.CairnIcon
import com.cairn.emergency.ui.settings.CairnLanguage
import com.cairn.emergency.ui.voice.rememberSpeechInput

private val ActionPurple = Color(0xFF5B37CF)

private enum class HouseholdSection(@StringRes val label: Int, val icon: CairnIcon, val tint: Color) {
    Members(R.string.household_members, CairnIcon.Household, Color(0xFF095C8D)),
    Pets(R.string.household_pets, CairnIcon.Paw, Color(0xFF095C8D)),
    Medical(R.string.household_medical, CairnIcon.Health, Color(0xFFD2333D)),
    Accessibility(R.string.household_accessibility, CairnIcon.Mobility, JourneyPurple),
}

@Composable
internal fun HouseholdScreen(
    model: HouseholdViewModel,
    lifecycle: Lifecycle,
    language: CairnLanguage,
    onKit: () -> Unit,
    onBack: () -> Unit,
) {
    val state = model.state
    var showDescription by rememberSaveable { mutableStateOf(false) }
    var editingSection by rememberSaveable { mutableStateOf<HouseholdSection?>(null) }
    val (speech, toggleSpeech) = rememberSpeechInput(lifecycle, language.tag, onTranscript = model::setDescription)
    val controlsEnabled = !state.busy && !speech.active && !state.loadFailed

    JourneyScaffold(
        title = stringResource(R.string.household_title),
        onBack = onBack,
        bottomBar = {
            Surface(color = JourneyPaper, shadowElevation = 1.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            showDescription = true
                            toggleSpeech()
                        },
                        enabled = !state.busy && !state.loadFailed,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        CairnGlyph(if (speech.active) CairnIcon.Stop else CairnIcon.Microphone, ActionPurple, Modifier.size(23.dp))
                        Text(
                            stringResource(if (speech.active) R.string.voice_stop else R.string.household_voice_update),
                            Modifier.padding(start = 10.dp),
                            color = JourneyNavy,
                        )
                    }
                    TextButton(
                        onClick = { showDescription = true },
                        enabled = controlsEnabled,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.household_by_text), color = JourneyNavy) }
                }
            }
        },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (state.loading) Status(R.string.household_loading)
            state.message?.let { Status(it) }
            if (state.loadFailed) {
                OutlinedButton(onClick = model::load, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.retry))
                }
            } else if (!state.loading) {
                val profile = state.draft ?: state.saved
                if (state.aiDraft) {
                    Text(stringResource(R.string.household_local_ai), color = Color(0xFF136A44), fontWeight = FontWeight.Bold)
                }
                if (state.editing && profile != null) {
                    HouseholdTextInput(state.description, model::setDescription, controlsEnabled, transcription = false)
                    Surface(shape = RoundedCornerShape(15.dp), color = Color.White, border = BorderStroke(1.dp, JourneyOutline)) {
                        ProfileEditor(profile, editingSection, controlsEnabled, model::updateDraft)
                    }
                } else {
                    HouseholdSection.entries.forEach { section ->
                        HouseholdRow(section, profile, controlsEnabled) {
                            editingSection = section
                            model.editProfile()
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            editingSection = null
                            model.editProfile()
                        },
                        enabled = controlsEnabled,
                        modifier = Modifier.fillMaxWidth().padding(top = 9.dp).heightIn(min = 50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF266196)),
                    ) {
                        Text(stringResource(R.string.household_edit), color = JourneyNavy, fontWeight = FontWeight.Bold)
                    }
                    if (state.draft == null) {
                        TextButton(onClick = onKit, enabled = controlsEnabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.household_open_kit), color = JourneyNavy, fontWeight = FontWeight.Bold)
                            CairnGlyph(CairnIcon.Next, JourneyTeal, Modifier.padding(start = 7.dp).size(18.dp))
                        }
                    }
                }

                if (showDescription && !state.editing) {
                    HouseholdTextInput(state.description, model::setDescription, controlsEnabled, transcription = true)
                    if (state.draft == null) {
                        Button(
                            onClick = { model.suggestProfile(language) },
                            enabled = controlsEnabled && state.description.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ActionPurple),
                        ) { Text(stringResource(if (state.generating) R.string.household_generating else R.string.household_suggest)) }
                    }
                }
                speech.status?.let { Status(it) }

                if (state.draft != null) {
                    Button(
                        onClick = model::save,
                        enabled = controlsEnabled,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ActionPurple),
                    ) { Text(stringResource(if (state.saving) R.string.household_saving else R.string.household_save)) }
                    TextButton(onClick = model::discardDraft, enabled = controlsEnabled, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.household_cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun HouseholdTextInput(description: String, onChange: (String) -> Unit, enabled: Boolean, transcription: Boolean) {
    OutlinedTextField(
        value = description,
        onValueChange = onChange,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(if (transcription) R.string.household_transcript else R.string.household_describe)) },
        placeholder = { Text(stringResource(R.string.household_example)) },
        minLines = 2,
        maxLines = 5,
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun HouseholdRow(section: HouseholdSection, profile: HouseholdProfile?, enabled: Boolean, onClick: () -> Unit) {
    val summary = when (section) {
        HouseholdSection.Members -> {
            val counts = listOf(profile?.adults, profile?.children, profile?.infants)
            if (counts.all { it != null }) {
                val people = counts.filterNotNull().sum()
                pluralStringResource(R.plurals.kit_people, people, people)
            } else profile?.members.orEmpty()
        }
        HouseholdSection.Pets -> profile?.petCount?.let { pluralStringResource(R.plurals.kit_pet_count, it, it) }
            ?: profile?.pets.orEmpty()
        HouseholdSection.Medical -> profile?.medicalNeeds.orEmpty()
        HouseholdSection.Accessibility -> profile?.accessibility.orEmpty()
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, JourneyOutline),
    ) {
        Row(Modifier.heightIn(min = 66.dp).padding(horizontal = 13.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            CairnGlyph(section.icon, section.tint, Modifier.size(28.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stringResource(section.label), color = JourneyNavy, fontWeight = FontWeight.Bold)
                Text(summary.ifBlank { stringResource(R.string.household_unknown) }, color = JourneyNavy, style = MaterialTheme.typography.bodySmall)
            }
            CairnGlyph(CairnIcon.Next, JourneyNavy, Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ProfileEditor(profile: HouseholdProfile, section: HouseholdSection?, enabled: Boolean, onChange: (HouseholdProfile) -> Unit) {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (section == null || section == HouseholdSection.Members) {
            CountInput(R.string.household_adults, profile.adults, enabled) { onChange(profile.copy(adults = it)) }
            CountInput(R.string.household_children, profile.children, enabled) { onChange(profile.copy(children = it)) }
            CountInput(R.string.household_infants, profile.infants, enabled) { onChange(profile.copy(infants = it)) }
            NoteInput(R.string.household_members, profile.members, enabled) { onChange(profile.copy(members = it)) }
            NoteInput(R.string.household_housing, profile.housing, enabled) { onChange(profile.copy(housing = it)) }
        }
        if (section == null || section == HouseholdSection.Pets) {
            CountInput(R.string.household_pets, profile.petCount, enabled) { onChange(profile.copy(petCount = it)) }
            NoteInput(R.string.household_pet_details, profile.pets, enabled) { onChange(profile.copy(pets = it)) }
        }
        if (section == null || section == HouseholdSection.Medical) {
            NoteInput(R.string.household_medical, profile.medicalNeeds, enabled) { onChange(profile.copy(medicalNeeds = it)) }
        }
        if (section == null || section == HouseholdSection.Accessibility) {
            NoteInput(R.string.household_accessibility, profile.accessibility, enabled) { onChange(profile.copy(accessibility = it)) }
        }
    }
}

@Composable
private fun CountInput(@StringRes label: Int, count: Int?, enabled: Boolean, onChange: (Int?) -> Unit) {
    OutlinedTextField(
        value = count?.toString().orEmpty(),
        onValueChange = { input ->
            if (input.isEmpty()) onChange(null)
            else input.toIntOrNull()?.takeIf { it in 0..MaximumHouseholdCount }?.let(onChange)
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        placeholder = { Text(stringResource(R.string.household_unknown)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
    )
}

@Composable
private fun NoteInput(@StringRes label: Int, note: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = note,
        onValueChange = { onChange(it.take(300)) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        maxLines = 3,
    )
}

@Composable
private fun Status(@StringRes message: Int) {
    Text(
        stringResource(message),
        Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        color = JourneyNavy,
        style = MaterialTheme.typography.bodySmall,
    )
}
