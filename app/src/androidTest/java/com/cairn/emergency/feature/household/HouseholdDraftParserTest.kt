package com.cairn.emergency.feature.household

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HouseholdDraftParserTest {
    private val result = """
        {
          "adults": 2,
          "children": null,
          "infants": 1,
          "petCount": 0,
          "members": "",
          "pets": "",
          "medicalNeeds": "",
          "housing": "Appartement",
          "accessibility": ""
        }
    """.trimIndent()

    @Test fun preservesUnknownAndZeroCountsAndLocalizedDetails() {
        val profile = requireNotNull(parseHouseholdDraft(result, "Deux adultes et un bébé."))
        assertEquals(2, profile.adults)
        assertNull(profile.children)
        assertEquals(1, profile.infants)
        assertEquals(0, profile.petCount)
        assertEquals("Appartement", profile.housing)
        assertEquals("Deux adultes et un bébé.", profile.originalDescription)
    }

    @Test fun rejectsFabricatedNumericTypesAndOutOfRangeCounts() {
        for (invalid in listOf("\"2\"", "2.5", "-1", "100")) {
            assertNull(parseHouseholdDraft(result.replace("\"adults\": 2", "\"adults\": $invalid"), ""))
        }
    }

    @Test fun rejectsMissingFieldsAndTrailingModelText() {
        assertNull(parseHouseholdDraft(result.replace("\"children\": null,", ""), ""))
        assertNull(parseHouseholdDraft("$result\nThis is my answer.", ""))
    }
}
