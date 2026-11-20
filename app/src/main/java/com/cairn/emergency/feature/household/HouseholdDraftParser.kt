package com.cairn.emergency.feature.household

import com.cairn.emergency.data.HouseholdProfile
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

internal const val MaximumHouseholdCount = 99
internal const val MaximumHouseholdDescription = 1500

internal fun parseHouseholdDraft(response: String, description: String): HouseholdProfile? {
    val cleaned = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    return try {
        val reader = JSONTokener(cleaned)
        val json = reader.nextValue() as? JSONObject ?: return null
        if (reader.nextClean() != '\u0000') return null
        fun count(key: String): Int? {
            val value = json.get(key)
            if (value === JSONObject.NULL) return null
            if (value !is Int || value !in 0..MaximumHouseholdCount) throw JSONException(key)
            return value
        }
        fun detail(key: String): String {
            val value = json.get(key) as? String ?: throw JSONException(key)
            if (value.length > 300) throw JSONException(key)
            return value.trim()
        }
        HouseholdProfile(
            members = detail("members"),
            medicalNeeds = detail("medicalNeeds"),
            pets = detail("pets"),
            housing = detail("housing"),
            accessibility = detail("accessibility"),
            originalDescription = description,
            adults = count("adults"),
            children = count("children"),
            infants = count("infants"),
            petCount = count("petCount"),
        )
    } catch (_: JSONException) {
        null
    }
}
