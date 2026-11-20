package com.cairn.emergency.ai

enum class Engine { LOCAL, CLOUD }

internal fun inferenceEngines(preferCloud: Boolean): List<Engine> =
    if (preferCloud) listOf(Engine.CLOUD, Engine.LOCAL) else listOf(Engine.LOCAL)

data class RoutedAnswer(val text: String, val engine: Engine?)
