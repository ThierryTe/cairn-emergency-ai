package com.cairn.emergency.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cairn.emergency.R
import com.cairn.emergency.feature.kit.kitPercentLabel
import com.cairn.emergency.ui.components.JourneyNavy
import com.cairn.emergency.ui.components.JourneyOutline
import com.cairn.emergency.ui.components.JourneyPaper
import com.cairn.emergency.ui.components.JourneyPurple
import com.cairn.emergency.ui.components.JourneyTeal

internal enum class CairnDestination { Home, Household, Kit, Assistant, Simulator, Sos, VoiceSos, Settings }

private val AccentGreen = Color(0xFF007E54)
private val SosOrange = Color(0xFFF36C45)
private val SosText = Color(0xFF351407)

private data class Tile(val label: Int, val icon: CairnIcon, val color: Color)

private val tiles = listOf(
    Tile(R.string.home_household, CairnIcon.Household, AccentGreen),
    Tile(R.string.home_kit, CairnIcon.Kit, AccentGreen),
    Tile(R.string.home_assistant, CairnIcon.Chat, JourneyPurple),
    Tile(R.string.home_simulator, CairnIcon.Mountain, JourneyPurple),
)

@Composable
internal fun CairnHome(
    onHousehold: () -> Unit,
    onKit: () -> Unit,
    onAssistant: () -> Unit,
    onSimulator: () -> Unit,
    onSos: () -> Unit,
    onTalkSos: () -> Unit,
    onSettings: () -> Unit,
    kitProgress: Int?,
    kitCountsIncomplete: Boolean,
) {
    Column(Modifier.fillMaxSize().background(JourneyPaper), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f).widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            HomeHero()
            Box(Modifier.background(Brush.verticalGradient(0f to Color(0xFF22718A), .55f to JourneyPaper))) {
                Surface(
                    onClick = onKit,
                    shape = RoundedCornerShape(topStart = 21.dp, topEnd = 21.dp, bottomStart = 17.dp, bottomEnd = 17.dp),
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    border = BorderStroke(1.dp, JourneyOutline),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(if (kitCountsIncomplete) R.string.kit_home_checklist_progress else R.string.kit_home_readiness),
                            color = JourneyNavy,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        if (kitProgress != null) {
                            Text(kitPercentLabel(kitProgress), color = JourneyNavy, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                            LinearProgressIndicator(
                                progress = { kitProgress / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = JourneyTeal,
                                trackColor = Color(0xFFE0E8F0),
                            )
                        } else {
                            Text(stringResource(R.string.home_view_kit), color = JourneyNavy, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                tiles.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        row.forEach { tile ->
                            val action = when (tile.icon) {
                                CairnIcon.Household -> onHousehold
                                CairnIcon.Kit -> onKit
                                CairnIcon.Chat -> onAssistant
                                CairnIcon.Mountain -> onSimulator
                                else -> null
                            }
                            HomeTile(tile, action, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Row(
            Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onSos,
                modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SosOrange, contentColor = SosText),
            ) {
                CairnGlyph(CairnIcon.Phone, SosText, Modifier.size(22.dp))
                Text(stringResource(R.string.sos_title), Modifier.padding(start = 9.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            val label = stringResource(R.string.voice_sos_talk)
            OutlinedIconButton(
                onClick = onTalkSos,
                modifier = Modifier.size(50.dp).semantics { contentDescription = label },
            ) { CairnGlyph(CairnIcon.Microphone, SosText, Modifier.size(23.dp)) }
        }
        HomeNavigation(onKit, onAssistant, onSettings)
    }
}

@Composable
private fun HomeHero() {
    Box(Modifier.fillMaxWidth().background(JourneyNavy)) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.verticalGradient(listOf(JourneyNavy, Color(0xFF073A50), Color(0xFF1B7084))))
            fun ridge(color: Color, vararg points: Pair<Float, Float>) {
                val shape = Path().apply {
                    moveTo(points.first().first * size.width, points.first().second * size.height)
                    for ((x, y) in points.drop(1)) lineTo(x * size.width, y * size.height)
                    close()
                }
                drawPath(shape, color)
            }
            ridge(
                Color(0xFF43899E), 0f to .88f, .07f to .79f, .19f to .91f, .27f to .84f, .4f to .98f,
                .68f to .78f, .86f to .61f, 1f to .79f, 1f to 1f, 0f to 1f,
            )
            ridge(
                Color(0xFFCEE8F0), .71f to .76f, .86f to .61f, .95f to .73f, .91f to .71f,
                .9f to .77f, .86f to .71f, .81f to .8f, .82f to .71f,
            )
            ridge(
                Color(0xFF22718A), 0f to .94f, .07f to .89f, .16f to .97f, .24f to .92f, .35f to .98f,
                .58f to .9f, .68f to .94f, .86f to .75f, .92f to .85f, 1f to .9f, 1f to 1f, 0f to 1f,
            )
        }
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CairnGlyph(CairnIcon.Mountain, Color(0xFF52C3D6), Modifier.size(45.dp))
                Text(stringResource(R.string.app_name), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            }
            Text(
                stringResource(R.string.home_positioning),
                modifier = Modifier.padding(top = 4.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HomeTile(tile: Tile, action: (() -> Unit)?, modifier: Modifier = Modifier) {
    val touch = if (action != null) Modifier.clickable(role = Role.Button, onClick = action) else Modifier.semantics { disabled() }
    Surface(
        modifier = modifier.heightIn(min = 90.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, JourneyOutline),
        color = Color.White,
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 90.dp).then(touch).padding(horizontal = 7.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CairnGlyph(tile.icon, tile.color, Modifier.size(32.dp))
            Text(
                stringResource(tile.label),
                modifier = Modifier.padding(top = 5.dp),
                color = JourneyNavy,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HomeNavigation(onKit: () -> Unit, onAssistant: () -> Unit, onSettings: () -> Unit) {
    val entries = listOf(
        R.string.home to CairnIcon.Home,
        R.string.nav_assistant to CairnIcon.Chat,
        R.string.nav_kit to CairnIcon.Kit,
        R.string.nav_more to CairnIcon.More,
    )
    Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.navigationBarsPadding(), contentAlignment = Alignment.Center) {
            Row(Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
                entries.forEach { (label, icon) ->
                    val state = when (icon) {
                        CairnIcon.Home -> Modifier.semantics { selected = true }
                        CairnIcon.Chat -> Modifier.clickable(role = Role.Tab, onClick = onAssistant)
                        CairnIcon.Kit -> Modifier.clickable(role = Role.Tab, onClick = onKit)
                        CairnIcon.More -> Modifier.clickable(role = Role.Tab, onClick = onSettings)
                        else -> Modifier.semantics { disabled() }
                    }
                    val color = if (icon == CairnIcon.Home) AccentGreen else JourneyNavy
                    Column(
                        Modifier.weight(1f).heightIn(min = 52.dp).then(state).padding(vertical = 5.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CairnGlyph(icon, color, Modifier.size(21.dp))
                        Text(stringResource(label), color = color, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
