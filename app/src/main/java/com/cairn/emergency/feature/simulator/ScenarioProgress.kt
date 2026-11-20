package com.cairn.emergency.feature.simulator

internal enum class Scenario(val id: String, private val safest: List<Int>) {
    Flood("flood", listOf(0, 1, 2, 0, 2, 1)),
    Power("power", listOf(1, 2, 0, 1, 0, 2));

    val length: Int get() = safest.size
    fun safestChoice(step: Int) = safest[step]
}

internal data class ScenarioProgress(
    val decisions: String = "",
    val feedbackPending: Boolean = false,
) {
    val step: Int get() = if (feedbackPending) decisions.lastIndex else decisions.length
    val chosen: Int? get() = if (feedbackPending) decisions.last().digitToInt() else null

    fun isFinished(scenario: Scenario) = decisions.length == scenario.length && !feedbackPending

    fun score(scenario: Scenario) = decisions.withIndex().count { (index, choice) ->
        choice.digitToInt() == scenario.safestChoice(index)
    }

    fun choose(scenario: Scenario, choice: Int): ScenarioProgress? {
        if (feedbackPending || isFinished(scenario) || choice !in 0..2) return null
        return copy(decisions = decisions + choice, feedbackPending = true)
    }

    fun continueAfterFeedback() = if (feedbackPending) copy(feedbackPending = false) else null

    companion object {
        fun restore(scenario: Scenario, decisions: String, feedbackPending: Boolean): ScenarioProgress? {
            if (decisions.length > scenario.length || decisions.any { it !in '0'..'2' }) return null
            if (feedbackPending && decisions.isEmpty()) return null
            return ScenarioProgress(decisions, feedbackPending)
        }
    }
}
