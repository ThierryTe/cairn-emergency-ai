package com.cairn.emergency.ui.sos

import java.text.Normalizer
import java.util.Locale

internal enum class SosCommand {
    OpenSos, Help, Message, Dialer, RescueCard, Locate, ReadLocation, Guidance, ReadGuidance, StopReading, Describe,
}

internal enum class SosIncident {
    Flood, HomeFire, PowerOutage, Earthquake, Injury,
}

internal data class SpokenEmergency(val incident: SosIncident?, val people: Int?, val urgentNeeds: String)

private val commandPatterns = listOf(
    SosCommand.OpenSos to "(?:open|show|go to|ouvre[z]?|affiche[z]?|abre|abrir|muestra) (?:my |the |le |mon |el |mi )?(?:emergency sos|sos|s o s)",
    SosCommand.Help to "i need help|help me|i need (?:emergency|urgent) help|j ai besoin d aide|aide moi|ayuda|necesito ayuda|ayudame",
    SosCommand.Message to "(?:prepare|draft|write|compose|create|preparez?|redige[z]?|ecris|prepara|redacta|escribe) (?:(?:a|an|my|the|un|une|mon|el|mi) )?(?:emergency )?(?:sms|text(?: message)?|message|mensaje)(?: (?:for|to|pour|a|para) (?:my|an|mon|un|mi) (?:emergency contact|contact d urgence|contacto de emergencia))?",
    SosCommand.Dialer to "(?:call|dial|ring|appelle[z]?|contacte[z]?|llama(?:r)?|marca) (?:the |an |les |aux |une |a |a los |a las |una )?(?:emergency services|first responders|police|ambulance|ambulancia|secours|urgences|emergencias|servicios de emergencia)",
    SosCommand.Dialer to "(?:open|show|ouvre[z]?|affiche[z]?|abre|muestra) (?:the |le |el )?(?:emergency dialer|phone keypad|clavier d urgence|clavier du telephone|marcador de emergencia|teclado del telefono)",
    SosCommand.RescueCard to "(?:show|open|display|pull up|affiche[z]?|montre[z]?|ouvre[z]?|muestra|abre) (?:my |the |ma |la |mi )?(?:rescue card|fiche de secours|tarjeta de rescate)",
    SosCommand.ReadLocation to "(?:read|say|read out|tell me|lis|lisez|dis moi|dites moi|lee|dime) (?:my |the |ma |mes |la |mi |mis )?(?:location|coordinates|position|coordonnees|ubicacion|coordenadas)(?: aloud| a voix haute| en voz alta)?",
    SosCommand.Locate to "where am i|what s my location|ou suis je|ou est ce que je suis|donde estoy|(?:add|share|find|get|show|ajoute[z]?|partage[z]?|trouve[z]?|affiche[z]?|anade|agrega|comparte|busca|muestra) (?:my |the |ma |mes |la |mi |mis )?(?:device )?(?:location|coordinates|position|coordonnees|ubicacion|coordenadas)(?: (?:to|in|a|dans|al|en) (?:my |the |mon |le |mi |el )?(?:message|draft|brouillon|mensaje|borrador|rescue card|fiche de secours|tarjeta de rescate))?",
    SosCommand.Guidance to "(?:open|show|find|ouvre[z]?|affiche[z]?|montre[z]?|abre|muestra) (?:the |les |le |las |la )?(?:offline guidance|emergency guidance|emergency instructions|flood instructions|fire instructions|earthquake instructions|power outage instructions|guides hors ligne|consignes d urgence|guias sin conexion|instrucciones de emergencia)",
    SosCommand.ReadGuidance to "(?:read|read out|lis|lisez|lee) (?:the |les |las )?(?:offline guidance|emergency guidance|emergency instructions|flood instructions|fire instructions|earthquake instructions|power outage instructions|guides hors ligne|consignes d urgence|guias sin conexion|instrucciones de emergencia)(?: aloud| a voix haute| en voz alta)?",
    SosCommand.StopReading to "stop(?: reading| speaking| the audio)?|arrete[z]?(?: de lire| la lecture)?|stop la lecture|deja de leer|deten(?: la lectura| el audio)?",
).map { (command, pattern) -> command to Regex(pattern) }

private val guidanceQuestion = Regex(
    "^(?:what should i do|what do i do|how do i stay safe|que faire|qu est ce que je dois faire|comment me proteger|que hago|que debo hacer|como me protejo)\\b"
)
private val addressedToCairn = Regex("^(?:(?:hey|ok|bonjour|hola) )?cairn(?: |$)")
private val politePrefix = Regex("^(?:please |can you |could you |would you |s il te plait |s il vous plait |peux tu |pouvez vous |por favor |puedes |podrias )+")
private val politeSuffix = Regex("(?: (?:now|please|maintenant|tout de suite|ahora|s il te plait|s il vous plait|por favor))+$")

internal fun parseSosCommand(input: String): SosCommand {
    val phrase = normalizeSos(input).replace(addressedToCairn, "").replace(politePrefix, "").replace(politeSuffix, "")
    return commandPatterns.firstOrNull { (_, pattern) -> pattern.matches(phrase) }?.first
        ?: if (guidanceQuestion.containsMatchIn(phrase)) SosCommand.Guidance else SosCommand.Describe
}

private val incidentTerms = mapOf(
    SosIncident.Flood to listOf("flood", "flooding", "floodwater", "flooded", "inondation", "inondee", "inonde", "inundacion", "inundada", "inundado", "inundando"),
    SosIncident.HomeFire to listOf("house fire", "home fire", "fire in", "on fire", "incendie", "incendio", "fuego en"),
    SosIncident.PowerOutage to listOf("power outage", "power is out", "blackout", "coupure de courant", "panne de courant", "apagon", "corte de luz"),
    SosIncident.Earthquake to listOf("earthquake", "seisme", "tremblement de terre", "terremoto", "sismo"),
    SosIncident.Injury to listOf("is injured", "am injured", "is bleeding", "est blesse", "est blessee", "saigne", "esta herido", "esta herida", "esta sangrando"),
)
private val clauseBoundary = Regex("[.!?;,:\\n]+|\\b(?:and|but|et|mais|y|pero)\\b", RegexOption.IGNORE_CASE)
private val explicitNegative = Regex(
    "\\b(?:no|not|never|pas|aucun|aucune|sin|niega|isn t|aren t|nobody|personne|nadie)\\b"
)
private val spokenNumbers = listOf(
    "one|un|une|uno|una", "two|deux|dos", "three|trois|tres", "four|quatre|cuatro", "five|cinq|cinco",
    "six|seis", "seven|sept|siete", "eight|huit|ocho", "nine|neuf|nueve", "ten|dix|diez",
)
private val countPattern = Regex(
    "\\b(?:we (?:are|re)|there are|nous sommes|on est|il y a|somos|hay) ([1-9][0-9]?|${spokenNumbers.joinToString("|")}) (?:people|persons|personnes|personas)\\b|" +
        "\\b(?:we (?:are|re)|nous sommes|on est|somos) ([1-9][0-9]?|${spokenNumbers.joinToString("|")})$"
)
private val urgentNeed = Regex(
    "\\b(?:cannot walk|can not walk|can t walk|cannot move|can t move|cannot breathe|can t breathe|needs oxygen|need oxygen|needs a wheelchair|needs help moving|is trapped|am trapped|is bleeding|" +
        "ne peut pas marcher|ne peux pas marcher|ne peut pas bouger|ne peut pas respirer|ne peux pas respirer|besoin d oxygene|besoin d un fauteuil roulant|est coince|est coincee|saigne|" +
        "no puede caminar|no puedo caminar|no puede moverse|no puede respirar|no puedo respirar|necesita oxigeno|necesito oxigeno|necesita silla de ruedas|esta atrapado|esta atrapada|esta sangrando)\\b"
)

internal fun interpretEmergency(input: String): SpokenEmergency {
    val originalClauses = input.trim().split(clauseBoundary).map(String::trim).filter(String::isNotBlank)
    val clauses = originalClauses.map(::normalizeSos)
    val incidents = incidentTerms.entries.filter { (_, terms) ->
        clauses.any { clause -> !explicitNegative.containsMatchIn(clause) && terms.any { " $it " in " $clause " } }
    }.map { it.key }
    val incident = incidents.filter { it != SosIncident.Injury }.ifEmpty { incidents }.singleOrNull()
    val counts = clauses.flatMap { clause ->
        if (explicitNegative.containsMatchIn(clause)) emptyList()
        else countPattern.findAll(clause).mapNotNull { match ->
            val value = match.groupValues[1].ifEmpty { match.groupValues[2] }
            value.toIntOrNull() ?: spokenNumbers.indexOfFirst { value in it.split('|') }.takeIf { it >= 0 }?.plus(1)
        }.toList()
    }
    val needs = originalClauses.zip(clauses).filter { (original, normalized) ->
        original.length <= 220 && urgentNeed.containsMatchIn(normalized) &&
            !Regex("\\b(?:nobody|personne|nadie|not bleeding|does not need|no necesita)\\b").containsMatchIn(normalized)
    }.take(2).joinToString("\n") { it.first }
    return SpokenEmergency(incident, counts.distinct().singleOrNull(), needs)
}

private fun normalizeSos(input: String): String = Normalizer.normalize(input.lowercase(Locale.ROOT), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z0-9]+"), " ").trim()
