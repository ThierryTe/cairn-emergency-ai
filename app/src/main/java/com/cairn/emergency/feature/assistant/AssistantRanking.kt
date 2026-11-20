package com.cairn.emergency.feature.assistant

import android.content.res.AssetManager
import com.cairn.emergency.ai.AiRouter
import com.cairn.emergency.ai.Engine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal suspend fun rankPublishedGuidance(
    question: String,
    plan: AssistantPlan,
    passages: List<String>,
    assets: AssetManager,
): Pair<List<Int>, Engine>? {
    val instructions = withContext(Dispatchers.IO) {
        assets.open("prompts/assistant-v1.txt").bufferedReader().use { it.readText() }
    }
    val prompt = buildString {
        appendLine(instructions)
        appendLine("Question: ${JSONObject.quote(question)}")
        passages.forEachIndexed { index, passage -> appendLine("${index + 1}. $passage") }
    }
    return AiRouter.generateValidated(prompt, plan.preferCloud) { plan.validateOrder(it, passages.size) }
}
