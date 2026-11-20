package com.cairn.emergency.feature.kit

import com.cairn.emergency.data.HouseholdProfile
import com.cairn.emergency.data.KitCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class KitPlanTest {
    private val household = HouseholdProfile(adults = 2, children = 0, infants = 1, petCount = 1)

    @Test fun linksHouseholdCountsAndCreatesFourCategories() {
        val kit = buildKitPlan(household, emptyList())
        val supplies = kit.items.map { it.supply }.toSet()

        assertEquals(18, kit.waterLiters)
        assertEquals(KitCategory.entries.toSet(), supplies.map { it.category }.toSet())
        assertTrue(supplies.containsAll(setOf(KitSupply.BabySupplies, KitSupply.PetFood, KitSupply.PetWater)))
        assertEquals(0, kit.readyPercent)
        assertFalse(kit.householdCountsIncomplete)
    }

    @Test fun usesPerPersonTargetWhenAnyCountIsUnconfirmed() {
        val kit = buildKitPlan(household.copy(children = null), emptyList())

        assertNull(kit.waterLiters)
        assertTrue(kit.householdCountsIncomplete)
    }

    @Test fun includesRelevantRowsForConfirmedMedicalAndAccessibilityDetails() {
        val kit = buildKitPlan(household.copy(medicalNeeds = "Medication supply", accessibility = "Spare wheelchair battery"), emptyList())
        val supplies = kit.items.map { it.supply }.toSet()

        assertTrue(supplies.containsAll(setOf(KitSupply.Medications, KitSupply.MedicalDocuments, KitSupply.AccessibilityAids)))
        assertEquals("Spare wheelchair battery", kit.accessibilityDetails)
    }

    @Test fun avoidsOptionalRowsForExplicitZeroAndLegacyUnknownPlaceholders() {
        val profile = household.copy(infants = 0, petCount = 0, medicalNeeds = "Not provided", accessibility = "Not provided")
        val kit = buildKitPlan(profile, emptyList())
        val supplies = kit.items.map { it.supply }.toSet()

        assertTrue(setOf(KitSupply.BabySupplies, KitSupply.PetFood, KitSupply.Medications, KitSupply.AccessibilityAids).none { it in supplies })
    }

    @Test fun recalculatesChangedItemsWithoutClearingIndependentChecks() {
        val kit = buildKitPlan(household, emptyList())
        val checkAll = kit.items.map { KitCheck(it.supply.id, it.planKey) }
        assertEquals(100, buildKitPlan(household, checkAll).readyPercent)

        val updated = buildKitPlan(household.copy(adults = 3), checkAll)

        assertEquals(24, updated.waterLiters)
        assertEquals(
            setOf(KitSupply.Water, KitSupply.Food, KitSupply.WaterContainer, KitSupply.Hygiene, KitSupply.EssentialDocuments),
            updated.items.filterNot { it.checked }.map { it.supply }.toSet(),
        )
        assertEquals((updated.items.size - 5) * 100 / updated.items.size, updated.readyPercent)
    }

    @Test fun keepsEquivalentLegacyWaterAndFoodChecksWithoutMarkingNewItems() {
        val checks = listOf(
            KitCheck("water", hash("1:6;1:2;1:0;1:1;1:1;")),
            KitCheck("food", hash("1:3;1:2;1:0;1:1;")),
            KitCheck("power", hash("")),
        )

        val updated = buildKitPlan(household, checks)

        assertEquals(setOf(KitSupply.Water, KitSupply.Food), updated.items.filter { it.checked }.map { it.supply }.toSet())
    }

    @Test fun unrelatedChangesDoNotResetTheChecklist() {
        val kit = buildKitPlan(household, emptyList())
        val checkAll = kit.items.map { KitCheck(it.supply.id, it.planKey) }

        val updated = buildKitPlan(household.copy(originalDescription = "Updated wording"), checkAll)

        assertEquals(100, updated.readyPercent)
    }

    private fun hash(canonical: String) =
        MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
}
