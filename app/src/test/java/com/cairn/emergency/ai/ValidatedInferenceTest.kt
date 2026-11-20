package com.cairn.emergency.ai

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatedInferenceTest {
    @Test
    fun fallsBackToLocalAfterCloudTimesOut() = runBlocking {
        val attempted = mutableListOf<Engine>()
        val result = validatedInference("prompt", true, 100, { engine, _ ->
            attempted += engine
            if (engine == Engine.CLOUD) awaitCancellation()
            RoutedAnswer("1", engine)
        }, String::toIntOrNull)

        assertEquals(listOf(Engine.CLOUD, Engine.LOCAL), attempted)
        assertEquals(1 to Engine.LOCAL, result)
    }

    @Test
    fun failsClosedWhenLocalIsUnavailableAndCloudIsDisallowed() = runBlocking {
        val attempted = mutableListOf<Engine>()
        val result = validatedInference("prompt", false, 500, { engine, _ ->
            attempted += engine
            null
        }, String::toIntOrNull)

        assertEquals(listOf(Engine.LOCAL), attempted)
        assertNull(result)
    }

    @Test
    fun rejectsInvalidPayloadAndUnverifiedEngine() = runBlocking {
        val result = validatedInference("prompt", true, 500, { engine, _ ->
            when (engine) {
                Engine.CLOUD -> RoutedAnswer("2", null)
                Engine.LOCAL -> RoutedAnswer("This text is not a number.", engine)
            }
        }, String::toIntOrNull)

        assertNull(result)
    }

    @Test
    fun returnsVerifiedCloudWithoutCallingLocal() = runBlocking {
        val attempted = mutableListOf<Engine>()
        val result = validatedInference("prompt", true, 500, { engine, prompt ->
            attempted += engine
            assertEquals("prompt", prompt)
            RoutedAnswer("2", engine)
        }, String::toIntOrNull)

        assertEquals(listOf(Engine.CLOUD), attempted)
        assertEquals(2 to Engine.CLOUD, result)
    }

    @Test
    fun propagatesParentCancellationWithoutAttemptingFallback() = runBlocking {
        val attempted = mutableListOf<Engine>()
        var timedOut = false
        try {
            withTimeout(50) {
                validatedInference("prompt", true, 5_000, { engine, _ ->
                    attempted += engine
                    withTimeout(5_000) { awaitCancellation() }
                }, String::toIntOrNull)
            }
        } catch (_: TimeoutCancellationException) {
            timedOut = true
        }

        assertTrue(timedOut)
        assertEquals(listOf(Engine.CLOUD), attempted)
    }
}
