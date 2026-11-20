package com.cairn.emergency.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull

internal suspend fun <T : Any> validatedInference(
    prompt: String,
    preferCloud: Boolean,
    timeoutMs: Long,
    run: suspend (Engine, String) -> RoutedAnswer?,
    validate: (String) -> T?,
): Pair<T, Engine>? {
    for (engine in inferenceEngines(preferCloud)) {
        val result = try {
            withTimeoutOrNull(timeoutMs) {
                val answer = run(engine, prompt)
                if (answer?.engine == engine) validate(answer.text) else null
            }
        } catch (_: TimeoutCancellationException) {
            // Let an individual model's shorter timeout fall through to the next route.
            currentCoroutineContext().ensureActive()
            null
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            null
        }
        if (result != null) return result to engine
    }
    return null
}
