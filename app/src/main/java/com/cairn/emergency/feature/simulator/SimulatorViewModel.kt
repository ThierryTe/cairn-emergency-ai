package com.cairn.emergency.feature.simulator

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cairn.emergency.R
import com.cairn.emergency.data.CairnDatabase
import com.cairn.emergency.data.SimulationRun
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal class SimulatorViewModel(application: Application) : AndroidViewModel(application) {
    private val runs by lazy { CairnDatabase.get(application).simulationRunDao() }

    var progress by mutableStateOf<Map<Scenario, ScenarioProgress>?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    @get:StringRes
    var errorMessage by mutableStateOf<Int?>(null)
        private set

    init { load() }

    fun load() = execute(R.string.sim_load_error) {
        val saved = runs.load().associateBy { it.scenarioId }
        progress = Scenario.entries.mapNotNull { scenario ->
            val row = saved[scenario.id] ?: return@mapNotNull null
            val restored = ScenarioProgress.restore(scenario, row.decisions, row.feedbackPending)
                ?: throw IllegalStateException("Invalid saved simulation choices")
            scenario to restored
        }.toMap()
    }

    fun start(scenario: Scenario) {
        if (progress?.containsKey(scenario) == false) write(scenario, ScenarioProgress())
    }

    fun choose(scenario: Scenario, choice: Int) {
        progress?.get(scenario)?.choose(scenario, choice)?.let { write(scenario, it) }
    }

    fun continueAfterFeedback(scenario: Scenario) {
        progress?.get(scenario)?.continueAfterFeedback()?.let { write(scenario, it) }
    }

    fun restart(scenario: Scenario) {
        if (progress?.get(scenario)?.isFinished(scenario) == true) write(scenario, ScenarioProgress())
    }

    private fun write(scenario: Scenario, next: ScenarioProgress) = execute(R.string.sim_save_error) {
        val current = progress ?: return@execute
        runs.save(SimulationRun(scenario.id, next.decisions, next.feedbackPending))
        progress = current + (scenario to next)
    }

    private fun execute(@StringRes error: Int, action: suspend () -> Unit) {
        if (busy) return
        busy = true
        errorMessage = null
        viewModelScope.launch {
            try {
                action()
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                errorMessage = error
            } finally {
                busy = false
            }
        }
    }
}
