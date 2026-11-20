package com.cairn.emergency.ui.sos

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.cairn.emergency.R
import com.cairn.emergency.data.HouseholdProfile
import com.cairn.emergency.ui.components.JourneyNavy
import com.cairn.emergency.ui.components.JourneyOutline
import com.cairn.emergency.ui.components.JourneyPurple
import com.cairn.emergency.ui.components.JourneyScaffold
import com.cairn.emergency.ui.home.CairnGlyph
import com.cairn.emergency.ui.home.CairnIcon
import com.cairn.emergency.ui.settings.CairnLanguage
import com.cairn.emergency.ui.voice.rememberReadAloud

internal val SosOrange = Color(0xFFF36C45)
internal val SosInk = Color(0xFF351407)

private enum class SosPage(@StringRes val title: Int) {
    Menu(R.string.sos_title), Voice(R.string.voice_sos_title), Help(R.string.sos_get_help), Guidance(R.string.sos_guidance), Rescue(R.string.sos_rescue),
}

@Composable
internal fun EmergencySosScreen(
    profile: HouseholdProfile?,
    language: CairnLanguage,
    lifecycle: Lifecycle,
    readAloud: Boolean,
    simplified: Boolean,
    onBack: () -> Unit,
    startWithVoice: Boolean = false,
) {
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf(if (startWithVoice) SosPage.Voice else SosPage.Menu) }
    var autoListen by rememberSaveable { mutableStateOf(startWithVoice) }
    var activeGuide by rememberSaveable { mutableStateOf<EmergencyGuide?>(null) }
    var readGuideOnOpen by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(SosDraft()) }
    var pending by remember { mutableStateOf<SosCommunication?>(null) }
    var status by remember { mutableStateOf<Int?>(null) }
    val location = rememberSosLocation()
    val voice = rememberReadAloud(lifecycle, language.tag, readAloud)
    val holder = rememberSaveableStateHolder()

    fun open(destination: SosPage) {
        voice?.stop()
        status = null
        page = destination
    }

    val returnToMenu = { open(SosPage.Menu) }
    val request: (SosCommunication) -> Unit = {
        status = null
        pending = it
    }
    BackHandler(enabled = page != SosPage.Menu, onBack = returnToMenu)
    holder.SaveableStateProvider(page.name) {
        JourneyScaffold(stringResource(page.title), if (page == SosPage.Menu) onBack else returnToMenu) {
            Column(Modifier.fillMaxSize()) {
                if (status != null) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) { SosStatus(status) }
                }
                when (page) {
                    SosPage.Menu -> SosMenu { selected ->
                        if (selected == SosPage.Voice) autoListen = true
                        if (selected == SosPage.Guidance) {
                            activeGuide = null
                            readGuideOnOpen = false
                        }
                        open(selected)
                    }
                    SosPage.Voice -> VoiceSos(
                        profile, draft, { draft = it }, language, lifecycle, location, voice, autoListen,
                        onAutoListenHandled = { autoListen = false },
                        onRequest = request,
                        onOpenSos = returnToMenu,
                        onHelp = { open(SosPage.Help) },
                        onRescue = { open(SosPage.Rescue) },
                        onGuidance = { guide, read ->
                            activeGuide = guide
                            readGuideOnOpen = read
                            open(SosPage.Guidance)
                        },
                    )
                    SosPage.Help -> GetHelp(location, request)
                    SosPage.Guidance -> OfflineGuidance(voice, simplified, activeGuide, readGuideOnOpen)
                    SosPage.Rescue -> SosContent { RescueCard(profile, draft, { draft = it }, location, voice, request) }
                }
            }
        }
    }
    pending?.let { action ->
        ConfirmSosCommunication(action, voice, onDismiss = { pending = null }) {
            if (pending == action) {
                pending = null
                status = context.performConfirmedSosAction(action)
            }
        }
    }
}

@Composable
private fun SosMenu(onOpen: (SosPage) -> Unit) {
    SosContent {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CairnGlyph(CairnIcon.Siren, SosOrange, Modifier.size(62.dp))
            Text(
                stringResource(R.string.sos_intro),
                color = JourneyNavy,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 15.dp),
            )
        }
        Button(
            onClick = { onOpen(SosPage.Voice) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = SosOrange, contentColor = SosInk),
        ) {
            CairnGlyph(CairnIcon.Microphone, SosInk, Modifier.size(24.dp))
            Text(stringResource(R.string.voice_sos_talk), Modifier.padding(start = 9.dp), fontWeight = FontWeight.Bold)
        }
        SosCard(R.string.sos_get_help, R.string.sos_get_help_desc, CairnIcon.Phone, SosOrange, border = SosOrange) { onOpen(SosPage.Help) }
        SosCard(R.string.sos_guidance, R.string.sos_guidance_desc, CairnIcon.Book, JourneyPurple) { onOpen(SosPage.Guidance) }
        SosCard(R.string.sos_rescue, R.string.sos_rescue_desc, CairnIcon.RescueCard, JourneyPurple) { onOpen(SosPage.Rescue) }
        SosDisclaimer()
    }
}

@Composable
private fun GetHelp(location: SosLocationAction, onRequest: (SosCommunication) -> Unit) {
    val context = LocalContext.current
    var showDraft by rememberSaveable { mutableStateOf(false) }
    var draft by remember { mutableStateOf(context.getString(R.string.sos_draft_template)) }
    var deviceLocation by remember { mutableStateOf<SosLocationFix?>(null) }
    val attached = deviceLocation?.takeIf { it.isFresh(android.os.SystemClock.elapsedRealtimeNanos()) }

    SosContent {
        SosCard(R.string.sos_call, R.string.sos_open_keypad, CairnIcon.Phone, SosOrange) { onRequest(SosCommunication.Dialer) }
        SosCard(R.string.sos_prepare_sms, R.string.sos_sms_contact, CairnIcon.Chat, SosOrange) { showDraft = true }
        SosLocationPanel(location, attached) {
            deviceLocation = it
            showDraft = true
        }
        if (deviceLocation != null && attached == null) SosStatus(R.string.voice_sos_location_stale)
        if (showDraft) {
            RescueField(R.string.sos_draft_label, draft, limit = 2500) { draft = it }
            Button(
                onClick = { onRequest(SosCommunication.Messages(draft, attached)) },
                enabled = draft.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.sos_open_messages)) }
        }
        SosDisclaimer()
    }
}

@Composable
internal fun SosContent(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

@Composable
internal fun SosCard(
    @StringRes title: Int,
    @StringRes detail: Int,
    icon: CairnIcon,
    tint: Color,
    border: Color = JourneyOutline,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        color = Color.White, border = BorderStroke(1.dp, border),
    ) {
        Row(Modifier.heightIn(min = 65.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CairnGlyph(icon, tint, Modifier.size(27.dp))
            Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                Text(stringResource(title), color = JourneyNavy, fontWeight = FontWeight.Bold)
                Text(stringResource(detail), color = JourneyNavy, style = MaterialTheme.typography.bodySmall)
            }
            CairnGlyph(CairnIcon.Next, JourneyNavy, Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun SosStatus(@StringRes status: Int?) {
    if (status != null) {
        Text(stringResource(status), Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = JourneyNavy)
    }
}

@Composable
internal fun SosDisclaimer() {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFFFF3D9), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.sos_disclaimer), Modifier.padding(12.dp), color = JourneyNavy, style = MaterialTheme.typography.bodySmall)
    }
}

internal fun Context.launchSosIntent(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: SecurityException) {
    false
}

internal fun Context.openSosMessages(draft: String): Boolean =
    launchSosIntent(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", draft))
