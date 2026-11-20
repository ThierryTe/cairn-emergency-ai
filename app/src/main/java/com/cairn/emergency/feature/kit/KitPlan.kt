package com.cairn.emergency.feature.kit

import com.cairn.emergency.data.HouseholdProfile
import com.cairn.emergency.data.KitCheck
import java.security.MessageDigest

internal enum class KitCategory { WaterFood, HealthHygiene, SafetyTools, DocumentsEssentials }

internal enum class KitSupply(val id: String, val category: KitCategory) {
    Water("water", KitCategory.WaterFood),
    Food("food", KitCategory.WaterFood),
    PetFood("pet_food", KitCategory.WaterFood),
    WaterContainer("water_container", KitCategory.WaterFood),
    PetWater("pet_water", KitCategory.WaterFood),
    FirstAid("first_aid", KitCategory.HealthHygiene),
    Hygiene("hygiene", KitCategory.HealthHygiene),
    Medications("medications", KitCategory.HealthHygiene),
    BabySupplies("baby_supplies", KitCategory.HealthHygiene),
    Flashlight("flashlight", KitCategory.SafetyTools),
    Radio("radio", KitCategory.SafetyTools),
    PowerBank("power_bank", KitCategory.SafetyTools),
    AccessibilityAids("accessibility_aids", KitCategory.SafetyTools),
    EssentialDocuments("essential_documents", KitCategory.DocumentsEssentials),
    Contacts("contacts", KitCategory.DocumentsEssentials),
    MedicalDocuments("medical_documents", KitCategory.DocumentsEssentials),
}

internal data class KitItem(
    val supply: KitSupply,
    val planKey: String,
    val checked: Boolean,
)

internal data class KitPlan(
    val items: List<KitItem>,
    val people: Int?,
    val petCount: Int?,
    val infantCount: Int?,
    val petSuppliesNeeded: Boolean,
    val medicalDetails: String?,
    val accessibilityDetails: String?,
) {
    val checkedCount: Int get() = items.count { it.checked }
    val readyPercent: Int get() = checkedCount * 100 / items.size
    val waterLiters: Int? get() = people?.times(WaterLitersPerPerson)
    val householdCountsIncomplete: Boolean get() = people == null

    companion object {
        const val Days = 3
        const val WaterLitersPerPerson = 6
    }
}

internal fun buildKitPlan(profile: HouseholdProfile?, checks: List<KitCheck>): KitPlan {
    val adults = profile?.adults?.takeIf { it >= 0 }
    val children = profile?.children?.takeIf { it >= 0 }
    val infants = profile?.infants?.takeIf { it >= 0 }
    val pets = profile?.petCount?.takeIf { it >= 0 }
    val people = if (adults != null && children != null && infants != null) {
        (adults + children + infants).takeIf { it > 0 }
    } else null
    val medical = profile?.medicalNeeds?.confirmedDetail()
    val accessibility = profile?.accessibility?.confirmedDetail()
    val petDetails = profile?.pets?.confirmedDetail()
    val hasPets = (pets ?: 0) > 0 || pets == null && petDetails != null
    val validChecks = checks.associate { it.itemId to it.planKey }
    val items = buildList {
        fun addItem(supply: KitSupply, vararg factors: Any?) {
            val key = planKey(*factors)
            add(KitItem(supply, key, validChecks[supply.id] == key))
        }
        // Preserve the stable IDs and target keys of the two equivalent legacy rows.
        addItem(KitSupply.Water, KitPlan.WaterLitersPerPerson, adults, children, infants, pets)
        addItem(KitSupply.Food, KitPlan.Days, adults, children, infants)
        if (hasPets) addItem(KitSupply.PetFood, KitPlan.Days, pets, petDetails)
        addItem(KitSupply.WaterContainer, people?.times(KitPlan.WaterLitersPerPerson))
        if (hasPets) addItem(KitSupply.PetWater, KitPlan.Days, pets, petDetails)
        addItem(KitSupply.FirstAid)
        addItem(KitSupply.Hygiene, people)
        if (medical != null) addItem(KitSupply.Medications, medical)
        if ((infants ?: 0) > 0) addItem(KitSupply.BabySupplies, KitPlan.Days, infants)
        addItem(KitSupply.Flashlight)
        addItem(KitSupply.Radio)
        addItem(KitSupply.PowerBank)
        if (accessibility != null) addItem(KitSupply.AccessibilityAids, accessibility)
        addItem(KitSupply.EssentialDocuments, adults, children, infants, profile?.members)
        addItem(KitSupply.Contacts)
        if (medical != null) addItem(KitSupply.MedicalDocuments, medical)
    }
    return KitPlan(items, people, pets, infants, hasPets, medical, accessibility)
}

private fun String.confirmedDetail(): String? =
    trim().takeIf { it.isNotEmpty() && !it.equals("Not provided", ignoreCase = true) }

private fun planKey(vararg factors: Any?): String {
    val canonical = factors.joinToString("") { factor ->
        val normalized = when (factor) {
            is String -> factor.trim()
            else -> factor?.toString()
        }
        if (normalized == null) "-;" else "${normalized.length}:$normalized;"
    }
    return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }
}
