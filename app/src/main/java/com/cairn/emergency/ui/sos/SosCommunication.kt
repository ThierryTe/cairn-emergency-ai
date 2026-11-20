package com.cairn.emergency.ui.sos

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.os.SystemClock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.cairn.emergency.R
import com.cairn.emergency.ui.voice.ReadAloud

internal sealed interface SosCommunication {
    data object Dialer : SosCommunication
    data class Messages(val body: String, val location: SosLocationFix? = null) : SosCommunication
    data class CopyCard(val body: String, val location: SosLocationFix? = null) : SosCommunication
}

@Composable
internal fun ConfirmSosCommunication(
    action: SosCommunication,
    voice: ReadAloud?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val (title, explanation, confirmLabel) = when (action) {
        SosCommunication.Dialer -> Triple(R.string.voice_sos_confirm_dial, R.string.voice_sos_confirm_dial_detail, R.string.sos_open_keypad)
        is SosCommunication.Messages -> Triple(R.string.voice_sos_confirm_sms, R.string.voice_sos_confirm_sms_detail, R.string.sos_open_messages)
        is SosCommunication.CopyCard -> Triple(R.string.voice_sos_confirm_copy, R.string.voice_sos_confirm_copy_detail, R.string.rescue_copy)
    }
    val prompt = stringResource(title)
    LaunchedEffect(action, voice?.ready) {
        if (voice?.ready == true) voice.play(prompt)
    }
    AlertDialog(
        onDismissRequest = { voice?.stop(); onDismiss() },
        title = { Text(prompt) },
        text = { Text(stringResource(explanation)) },
        confirmButton = {
            TextButton(onClick = { voice?.stop(); onConfirm() }) { Text(stringResource(confirmLabel)) }
        },
        dismissButton = {
            TextButton(onClick = { voice?.stop(); onDismiss() }) { Text(stringResource(R.string.household_cancel)) }
        },
    )
}

// UI confirmation is the only caller. The system app still owns dialing and sending.
internal fun Context.performConfirmedSosAction(action: SosCommunication): Int? = when (action) {
    SosCommunication.Dialer -> if (launchSosIntent(Intent(Intent.ACTION_DIAL))) null else R.string.sos_no_dialer
    is SosCommunication.Messages -> when {
        action.location?.isFresh(SystemClock.elapsedRealtimeNanos()) == false -> R.string.voice_sos_location_stale
        openSosMessages(appendSosLocation(action.body, action.location)) -> null
        else -> R.string.sos_no_messages
    }
    is SosCommunication.CopyCard -> when {
        action.location?.isFresh(SystemClock.elapsedRealtimeNanos()) == false -> R.string.voice_sos_location_stale
        copySosCard(appendSosLocation(action.body, action.location)) -> R.string.rescue_copied
        else -> R.string.rescue_clipboard_error
    }
}

internal fun Context.appendSosLocation(body: String, location: SosLocationFix?): String =
    if (location == null) body else listOf(
        body,
        getString(R.string.sos_location_line, formatSosLocation(location)),
    ).filter(String::isNotBlank).joinToString("\n\n")

private fun Context.copySosCard(body: String): Boolean {
    val clipboard = getSystemService(ClipboardManager::class.java) ?: return false
    val clip = ClipData.newPlainText(getString(R.string.sos_rescue), body)
    if (Build.VERSION.SDK_INT >= 33) {
        clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    }
    return try {
        clipboard.setPrimaryClip(clip)
        true
    } catch (_: SecurityException) {
        false
    }
}
