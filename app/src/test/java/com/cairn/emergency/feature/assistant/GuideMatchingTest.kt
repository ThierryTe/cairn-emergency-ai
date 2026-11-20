package com.cairn.emergency.feature.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideMatchingTest {
    @Test fun findsGuideAcrossLanguages() {
        assertEquals(GuideTopic.Flood, matchGuide("What should I do during a flood?").topic)
        assertEquals(GuideTopic.Earthquake, matchGuide("Que faire pendant un séisme ?").topic)
        assertEquals(GuideTopic.Power, matchGuide("¿Qué hago durante un APAGÓN?").topic)
    }

    @Test fun unknownAndAmbiguousQuestionsHaveNoGuide() {
        for (question in listOf("How do I repair a roof?", "wildfire planning", "power outage during a flood")) {
            assertNull(question, matchGuide(question).topic)
        }
    }

    @Test fun identifiesUrgencyWithoutInventingAMatchingGuide() {
        val danger = matchGuide("I smell a gas smell during this blackout")
        assertTrue(danger.urgent)
        assertNull(danger.topic)
        assertTrue(matchGuide("Hay humo en la casa").urgent)
    }

    @Test fun selectsStorageWithoutAnsweringDosingQuestions() {
        for (question in listOf(
            "Power is out. How do I keep insulin cold?",
            "Comment garder l’insuline au frais ?",
            "¿Cómo conservar la insulina durante un apagón?",
        )) {
            assertEquals(question, GuideTopic.InsulinStorage, matchGuide(question).topic)
        }
        for (question in listOf(
            "What insulin dose should I take if the refrigerator stopped?",
            "Coupure de courant, quelle posologie ?",
            "¿Cuántas unidades de insulina si está fría?",
            "Can I change my insulin when the fridge fails?",
        )) {
            assertNull(question, matchGuide(question).topic)
        }
    }

    @Test fun acceptsOnlyACompletePermutationOfPublishedSteps() {
        assertEquals(listOf(2, 0, 1), parseStepOrder(" 3, 1, 2 ", 3))
        for (answer in listOf("1,1,2", "1,2", "0,1,2", "1,2,4", "1,2,3,4", "1,2,3\nTake a dose", "01,2,3")) {
            assertNull(answer, parseStepOrder(answer, 3))
        }
    }
}
