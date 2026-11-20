package com.cairn.emergency.ui.sos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SosVoiceParserTest {
    @Test
    fun extractsOnlyReportedFactsFromAnEmergencyDescription() {
        val words = "Cairn, there is flooding in my house. We are three people and my mother cannot walk."
        assertEquals(SosCommand.Describe, parseSosCommand(words))
        assertEquals(SpokenEmergency(SosIncident.Flood, 3, "my mother cannot walk"), interpretEmergency(words))
        assertEquals(SpokenEmergency(SosIncident.HomeFire, 2, "mon père ne peut pas marcher"),
            interpretEmergency("Il y a un incendie. Nous sommes deux personnes et mon père ne peut pas marcher."))
        assertEquals(SpokenEmergency(SosIncident.Flood, 4, "mi madre no puede caminar"),
            interpretEmergency("Hay una inundación. Somos cuatro personas y mi madre no puede caminar."))
    }

    @Test
    fun recognizesCommandsWithPoliteOpenersAndDifferentWording() {
        val cases = mapOf(
            "Hey Cairn, can you open the SOS, please?" to SosCommand.OpenSos,
            "I need emergency help!" to SosCommand.Help,
            "Compose an emergency text for my emergency contact" to SosCommand.Message,
            "Prepare a message for my emergency contact" to SosCommand.Message,
            "Call emergency services now" to SosCommand.Dialer,
            "Appelle les secours maintenant !" to SosCommand.Dialer,
            "Llama a los servicios de emergencia" to SosCommand.Dialer,
            "Pull up my Rescue Card" to SosCommand.RescueCard,
            "¿Dónde estoy?" to SosCommand.Locate,
            "Add my location to the message" to SosCommand.Locate,
            "Share my location" to SosCommand.Locate,
            "Read my coordinates aloud" to SosCommand.ReadLocation,
            "Read the emergency instructions" to SosCommand.ReadGuidance,
            "Read flood instructions" to SosCommand.ReadGuidance,
            "Stop reading" to SosCommand.StopReading,
            "Open offline guidance" to SosCommand.Guidance,
            "What should I do during a flood?" to SosCommand.Guidance,
        )
        cases.forEach { (phrase, expected) -> assertEquals(phrase, expected, parseSosCommand(phrase)) }
    }

    @Test
    fun negatedAndReportedInstructionsDoNotProposeSensitiveActions() {
        for (phrase in listOf(
            "Do not call emergency services.",
            "Don't share my location.",
            "Ne partage pas ma position.",
            "No llames a emergencias.",
            "My neighbor said call emergency services.",
            "Call emergency services and send my location.",
        )) {
            assertEquals(phrase, SosCommand.Describe, parseSosCommand(phrase))
        }
    }

    @Test
    fun leavesAmbiguousCountsAndNegatedIncidentsUnfilled() {
        assertEquals(SpokenEmergency(null, null, ""), interpretEmergency("There is no flooding. Nobody is bleeding."))
        assertEquals(SpokenEmergency(null, null, ""), interpretEmergency("We are three people, but we are four people."))
        assertNull(interpretEmergency("We are OK with my grandmother.").people)
        assertEquals("", interpretEmergency("I have diabetes. We are three people.").urgentNeeds)
    }

    @Test
    fun locationSelectionPrefersFreshAccurateDeviceFixesAndRejectsOldOrInvalidFixes() {
        val now = 200_000_000_000L
        val accurate = SosLocationFix(48.5, 2.2, 1730000000000L, now - 15_000_000_000L, 12f)
        val inaccurate = accurate.copy(elapsedRealtimeNanos = now - 2_000_000_000L, accuracyMeters = 200f)
        val expired = accurate.copy(elapsedRealtimeNanos = now - 120_000_000_000L, accuracyMeters = 2f)
        val invalid = accurate.copy(latitude = Double.NaN, accuracyMeters = 7f)
        assertEquals(accurate, bestSosLocation(listOf(inaccurate, expired, invalid, accurate), now))
        assertFalse(expired.isFresh(now))
        assertFalse(accurate.copy(elapsedRealtimeNanos = now + 1).isFresh(now))
        assertNull(bestSosLocation(listOf(expired, invalid), now))
    }
}
