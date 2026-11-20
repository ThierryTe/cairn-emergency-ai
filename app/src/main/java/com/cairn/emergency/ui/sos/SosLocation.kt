package com.cairn.emergency.ui.sos

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.cairn.emergency.R
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

internal data class SosLocationAction(
    val fix: SosLocationFix?,
    val busy: Boolean,
    @StringRes val status: Int?,
    val request: (askPermission: Boolean) -> Unit,
)

@Composable
@SuppressLint("MissingPermission")
internal fun rememberSosLocation(): SosLocationAction {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fix by remember { mutableStateOf<SosLocationFix?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<Int?>(null) }
    val permissions = remember { arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) }
    fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(fix) {
        val acquired = fix ?: return@LaunchedEffect
        while (acquired.isFresh(SystemClock.elapsedRealtimeNanos())) delay(1_000)
        fix = null
        if (!busy) status = R.string.voice_sos_location_stale
    }

    fun findLocation() {
        if (busy) return
        val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!fine && !granted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            status = R.string.sos_location_permission
            return
        }
        val manager = context.getSystemService(LocationManager::class.java)
        if (manager == null) {
            status = R.string.sos_location_unavailable
            return
        }
        busy = true
        status = R.string.sos_location_wait
        scope.launch {
            try {
                val candidates = buildList {
                    if (fine) add(LocationManager.GPS_PROVIDER)
                    if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
                    add(LocationManager.NETWORK_PROVIDER)
                }
                val providers = manager.getProviders(true).filter { it in candidates }
                val cached = providers.mapNotNull { provider -> readProvider { manager.getLastKnownLocation(provider)?.toSosFix() } }
                fix = bestSosLocation(cached + listOfNotNull(fix), SystemClock.elapsedRealtimeNanos())
                val direct = coroutineScope {
                    providers.map { provider -> async {
                        withTimeoutOrNull(16_000) { manager.currentSosFix(context, provider) }
                    } }.awaitAll().filterNotNull()
                }
                val samples = direct + cached + listOfNotNull(fix)
                val current = bestSosLocation(samples, SystemClock.elapsedRealtimeNanos())
                fix = current
                status = when {
                    current != null -> null
                    samples.isNotEmpty() -> R.string.voice_sos_location_stale
                    else -> R.string.sos_location_unavailable
                }
            } catch (_: SecurityException) {
                fix = null
                status = R.string.sos_location_permission
            } catch (_: IllegalArgumentException) {
                status = R.string.sos_location_unavailable
            } finally {
                busy = false
            }
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { findLocation() }
    return SosLocationAction(fix, busy, status) { ask ->
        if (!busy) {
            if (permissions.any(::granted)) findLocation()
            else if (ask) launcher.launch(permissions)
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun LocationManager.currentSosFix(context: Context, provider: String): SosLocationFix? =
    suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }
        try {
            LocationManagerCompat.getCurrentLocation(this, provider, signal, ContextCompat.getMainExecutor(context)) {
                continuation.resume(it?.toSosFix())
            }
        } catch (_: IllegalArgumentException) {
            continuation.resume(null)
        } catch (_: SecurityException) {
            continuation.resume(null)
        }
    }

private inline fun readProvider(read: () -> SosLocationFix?): SosLocationFix? = try {
    read()
} catch (_: IllegalArgumentException) {
    null
} catch (_: SecurityException) {
    null
}

private fun Location.toSosFix() = SosLocationFix(
    latitude, longitude, time, elapsedRealtimeNanos, accuracy.takeIf { hasAccuracy() && it.isFinite() && it > 0 },
)

internal fun Context.formatSosLocation(fix: SosLocationFix): String {
    val coordinates = String.format(Locale.US, "%.5f, %.5f", fix.latitude, fix.longitude)
    val locale = resources.configuration.locales[0]
    val updated = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale).format(Date(fix.timestampMs))
    val accuracy = fix.accuracyMeters?.let { getString(R.string.voice_sos_location_accuracy, it.roundToInt().coerceAtLeast(1)) }
    return listOfNotNull(coordinates, getString(R.string.voice_sos_location_time, updated), accuracy).joinToString("\n")
}
