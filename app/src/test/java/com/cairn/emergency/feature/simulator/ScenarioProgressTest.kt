package com.cairn.emergency.feature.simulator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioProgressTest {
    @Test fun eachScenarioPlaysThroughSixStepsAndScoresPublishedChoices() {
        for (scenario in Scenario.entries) {
            assertEquals(6, scenario.length)
            var run = ScenarioProgress()
            repeat(scenario.length) { step ->
                assertEquals(step, run.step)
                run = checkNotNull(run.choose(scenario, scenario.safestChoice(step)))
                assertTrue(run.feedbackPending)
                assertNull(run.choose(scenario, 2))
                assertFalse(run.isFinished(scenario))
                run = checkNotNull(run.continueAfterFeedback())
            }
            assertTrue(run.isFinished(scenario))
            assertNull(run.choose(scenario, 0))
            assertEquals(6, run.score(scenario))
        }
    }

    @Test fun canResumeWithAChoicePendingReview() {
        val run = checkNotNull(ScenarioProgress.restore(Scenario.Flood, "211", feedbackPending = true))
        assertEquals(2, run.step)
        assertEquals(1, run.chosen)
        assertEquals(1, run.score(Scenario.Flood))
        assertEquals(3, run.continueAfterFeedback()?.step)
    }

    @Test fun rejectsInvalidInputAndInvalidPersistedData() {
        assertNull(ScenarioProgress().choose(Scenario.Power, 3))
        assertNull(ScenarioProgress.restore(Scenario.Power, "", feedbackPending = true))
        assertNull(ScenarioProgress.restore(Scenario.Power, "03", feedbackPending = false))
        assertNull(ScenarioProgress.restore(Scenario.Power, "0120120", feedbackPending = false))
    }
}
