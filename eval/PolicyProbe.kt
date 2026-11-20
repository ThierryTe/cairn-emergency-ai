package com.cairn.eval

import com.cairn.emergency.ai.Engine
import com.cairn.emergency.ai.inferenceEngines
import com.cairn.emergency.feature.assistant.planAssistant
import java.util.Base64

fun main() {
    val decoder = Base64.getDecoder()
    generateSequence(::readlnOrNull).forEach { line ->
        val fields = line.split('\t')
        val question = decoder.decode(fields[0]).toString(Charsets.UTF_8)
        val plan = planAssistant(question, cloudAllowed = fields[1].toBooleanStrict(), online = fields[2].toBooleanStrict())
        val steps = fields[3].toInt()
        val routes = if (plan.match.topic == null) emptyList() else inferenceEngines(plan.preferCloud)
        val replies = mapOf(Engine.LOCAL to (fields[4] to fields[5]), Engine.CLOUD to (fields[6] to fields[7]))
        val accepted = routes.firstNotNullOfOrNull { route ->
            val (source, encoded) = replies.getValue(route)
            if (source != route.name) return@firstNotNullOfOrNull null
            val output = decoder.decode(encoded).toString(Charsets.UTF_8)
            plan.validateOrder(output, steps)?.let { route to it }
        }
        val result = when {
            plan.match.topic == null -> "NO_GUIDE"
            accepted != null -> accepted.first.name
            else -> "OFFLINE_GUIDES"
        }
        val order = if (plan.match.topic == null) emptyList() else accepted?.second ?: (0 until steps).toList()
        println(listOf(plan.match.topic?.name ?: "-", plan.match.urgent, result, order.joinToString(","), routes.joinToString(",")).joinToString("\t"))
    }
}
