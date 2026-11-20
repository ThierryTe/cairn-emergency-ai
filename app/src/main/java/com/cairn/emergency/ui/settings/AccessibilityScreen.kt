package com.cairn.emergency.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cairn.emergency.R
import com.cairn.emergency.ui.components.JourneyNavy
import com.cairn.emergency.ui.components.JourneyOutline
import com.cairn.emergency.ui.components.JourneyScaffold
import com.cairn.emergency.ui.home.CairnGlyph
import com.cairn.emergency.ui.home.CairnIcon

@Composable
internal fun AccessibilityScreen(
    language: CairnLanguage,
    preferences: AccessibilityViewModel,
    onLanguage: (CairnLanguage) -> Unit,
    onBack: () -> Unit,
) {
    var showLanguages by rememberSaveable { mutableStateOf(false) }
    val teal = Color(0xFF087D6D)
    JourneyScaffold(stringResource(R.string.settings_accessibility), onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Surface(
                onClick = { showLanguages = true },
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, JourneyOutline),
            ) {
                Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    CairnGlyph(CairnIcon.Language, JourneyNavy, Modifier.size(22.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(stringResource(R.string.settings_language), color = JourneyNavy, fontWeight = FontWeight.Bold)
                        Text(stringResource(language.nativeName), color = JourneyNavy, style = MaterialTheme.typography.bodySmall)
                    }
                    CairnGlyph(CairnIcon.Next, JourneyNavy, Modifier.size(18.dp))
                }
            }
            AccessibilityOption.entries.forEach { option ->
                val checked = preferences.enabled(option)
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, JourneyOutline)) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 58.dp)
                            .toggleable(checked, enabled = preferences.loaded, role = Role.Switch) { preferences.set(option, it) }
                            .padding(horizontal = 13.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CairnGlyph(option.icon, JourneyNavy, Modifier.size(22.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(stringResource(option.title), color = JourneyNavy, fontWeight = FontWeight.Bold)
                            option.subtitle?.let {
                                Text(stringResource(it), color = JourneyNavy, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Switch(
                            checked = checked, onCheckedChange = null, enabled = preferences.loaded,
                            colors = SwitchDefaults.colors(checkedTrackColor = teal),
                        )
                    }
                }
            }
            if (preferences.storageIssue) {
                Text(stringResource(R.string.settings_storage_issue), color = JourneyNavy)
            }
        }
    }
    if (showLanguages) {
        AlertDialog(
            onDismissRequest = { showLanguages = false },
            title = { Text(stringResource(R.string.settings_language)) },
            text = {
                Column(Modifier.selectableGroup()) {
                    CairnLanguage.entries.forEach { option ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .selectable(selected = option == language, role = Role.RadioButton) {
                                    showLanguages = false
                                    onLanguage(option)
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(option.nativeName), Modifier.weight(1f), color = JourneyNavy)
                            RadioButton(selected = option == language, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = teal))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguages = false }) { Text(stringResource(R.string.kit_close)) } },
        )
    }
}
