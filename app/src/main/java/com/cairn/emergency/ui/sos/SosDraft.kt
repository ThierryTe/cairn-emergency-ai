package com.cairn.emergency.ui.sos

internal const val MaxSosDescription = 650

private const val FreshLocationNanos = 120_000_000_000L

internal data class SosLocationFix(
    val latitude: Double,
    val longitude: Double,
    val timestampMs: Long,
    val elapsedRealtimeNanos: Long,
    val accuracyMeters: Float?,
) {
    fun isFresh(nowNanos: Long): Boolean {
        val age = nowNanos - elapsedRealtimeNanos
        return timestampMs > 0 && elapsedRealtimeNanos > 0 && age in 0 until FreshLocationNanos &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
}

internal fun bestSosLocation(fixes: List<SosLocationFix>, nowNanos: Long): SosLocationFix? = fixes
    .filter { it.isFresh(nowNanos) }
    .minWithOrNull(compareBy<SosLocationFix> { it.accuracyMeters ?: Float.POSITIVE_INFINITY }.thenByDescending { it.elapsedRealtimeNanos })

internal data class SosDraft(
    val incident: String = "",
    val people: String = "",
    val urgentNeeds: String = "",
    val notes: String = "",
    val manualLocation: String = "",
    val deviceLocation: SosLocationFix? = null,
) {
    val hasContent get() = listOf(incident, people, urgentNeeds, notes, manualLocation).any(String::isNotBlank) || deviceLocation != null
}
