package com.cairn.emergency.feature.assistant

import java.text.Normalizer
import java.util.Locale

internal enum class GuideTopic(private vararg val keywords: String) {
    Flood("flood", "flooding", "floodwater", "inondation", "inondations", "crue", "inundación", "inundaciones"),
    Power("power outage", "power cut", "power is out", "blackout", "coupure de courant", "panne de courant", "apagón", "corte de luz", "corte eléctrico"),
    Fire("fire", "smoke", "incendie", "feu", "fumée", "incendio", "fuego", "humo"),
    Earthquake("earthquake", "tremor", "séisme", "tremblement de terre", "terremoto", "sismo"),
    InsulinStorage;

    private val terms = keywords.map(::normalize)

    fun matches(text: String) = terms.any { " $it " in text }
}

internal data class GuideMatch(val topic: GuideTopic?, val urgent: Boolean)

internal data class AssistantPlan(
    val match: GuideMatch,
    val preferCloud: Boolean,
    val preserveFirstStep: Boolean,
) {
    fun validateOrder(output: String, stepCount: Int): List<Int>? {
        val order = parseStepOrder(output, stepCount) ?: return null
        return if (preserveFirstStep) listOf(0) + order.filter { it != 0 } else order
    }
}

internal fun planAssistant(question: String, cloudAllowed: Boolean, online: Boolean): AssistantPlan {
    val match = matchGuide(question)
    val preserveFirstStep = match.urgent || match.topic == GuideTopic.InsulinStorage
    val preferCloud = match.topic != null && cloudAllowed && online && !preserveFirstStep
    return AssistantPlan(match, preferCloud, preserveFirstStep)
}

private val urgentTerms = listOf(
    "bleeding", "saignement", "saigne", "sangrado", "sangrando", "chest pain", "pain in my chest",
    "douleur thoracique", "mal à la poitrine", "dolor de pecho", "dolor en el pecho",
    "gas smell", "smell gas", "odor of gas", "odeur de gaz", "olor a gas", "drowning", "noyade", "ahogamiento",
    "trapped", "coincé", "atrapado", "immediate danger", "danger immédiat", "peligro inmediato",
).map(::normalize)

private val insulinTerms = listOf("insulin", "insuline", "insulina")
private val storageTerms = listOf(
    "cold", "cool", "refrigerate", "refrigerator", "fridge", "store", "storage", "freezing", "heat",
    "au frais", "conserver", "conservation", "réfrigérateur", "frigo", "chaleur", "congélation",
    "conservar", "refrigerar", "frío", "fresca", "refrigerador", "calor", "congelación",
).map(::normalize)
private val medicineTerms = listOf(
    "dose", "doses", "dosage", "dosis", "posologie", "units", "unités", "unidades",
    "dosificar", "prescription", "inject", "injection", "injecter", "inyectar", "iu", "ui",
    "change my insulin", "switch insulin", "changer d’insuline", "cambiar de insulina",
    "medication", "médicament", "medicamento",
).map(::normalize)

private val harmfulPhrases = listOf(
    "set a house on fire", "start a house fire", "start an indoor fire", "set fire to", "commit arson",
    "mettre le feu à", "déclencher un incendie", "provoquer un incendie",
    "prender fuego a", "provocar un incendio", "incendiar una casa",
).map(::normalize)

internal fun matchGuide(question: String): GuideMatch {
    val normalized = " ${normalize(question)} "
    val topics = GuideTopic.entries.filter { it.matches(normalized) }
    val needsEmergencyHelp = urgentTerms.any { " $it " in normalized }
    val urgent = needsEmergencyHelp || topics.any { it != GuideTopic.Power }
    val topic = when {
        needsEmergencyHelp || medicineTerms.any { " $it " in normalized } || harmfulPhrases.any { " $it " in normalized } -> null
        insulinTerms.any { " $it " in normalized } ->
            GuideTopic.InsulinStorage.takeIf { storageTerms.any { term -> " $term " in normalized } && !urgent }
        else -> topics.singleOrNull()
    }
    return GuideMatch(topic, urgent)
}

internal fun parseStepOrder(text: String, size: Int): List<Int>? {
    if (text.length > 64) return null
    val indices = text.trim().split(',').map { part ->
        part.trim().takeIf { it.matches(Regex("[1-9][0-9]*")) }?.toIntOrNull()?.minus(1) ?: return null
    }
    return indices.takeIf { it.size == size && it.toSet() == (0 until size).toSet() }
}

private fun normalize(input: String): String = Normalizer.normalize(input.lowercase(Locale.ROOT), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z0-9]+"), " ").trim()
