package com.cairn.emergency.feature.assistant

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cairn.emergency.ai.Engine
import com.cairn.emergency.ui.sos.EmergencyGuide
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal data class AssistantTurn(
    val id: Long,
    val question: String,
    val match: GuideMatch,
    val order: List<Int>? = null,
    val engine: Engine? = null,
)

internal class AssistantViewModel(application: Application) : AndroidViewModel(application) {
    private var nextTurn = 0L
    var turns by mutableStateOf(emptyList<AssistantTurn>())
        private set
    var ranking by mutableStateOf(false)
        private set

    fun ask(question: String, context: Context, simplified: Boolean, allowCloud: Boolean) {
        val submitted = question.trim()
        if (submitted.isEmpty() || ranking) return
        val plan = planAssistant(submitted, allowCloud, allowCloud && isConnected(context))
        val turn = AssistantTurn(++nextTurn, submitted, plan.match)
        turns = turns.takeLast(7) + turn
        val topic = turn.match.topic ?: return
        val guide = EmergencyGuide.entries.first { it.topic == topic }
        val passages = context.resources.getStringArray(guide.stepsFor(simplified))
        ranking = true
        viewModelScope.launch {
            try {
                rankPublishedGuidance(submitted, plan, passages.toList(), context.assets)?.let { (order, engine) ->
                    turns = turns.dropLast(1) + turn.copy(order = order, engine = engine)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                // The source passages are already visible and usable.
            } finally {
                ranking = false
            }
        }
    }

    private fun isConnected(context: Context): Boolean {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    }
}
