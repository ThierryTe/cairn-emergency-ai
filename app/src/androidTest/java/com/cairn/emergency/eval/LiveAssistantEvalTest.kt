package com.cairn.emergency.eval

import android.content.res.Configuration
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cairn.emergency.ai.Engine
import com.cairn.emergency.ai.installFirebaseAppCheck
import com.cairn.emergency.feature.assistant.planAssistant
import com.cairn.emergency.feature.assistant.rankPublishedGuidance
import com.cairn.emergency.ui.sos.EmergencyGuide
import com.google.firebase.FirebaseApp
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveAssistantEvalTest {
    @Test
    fun evaluatePublishedGuidePrompts() {
        val mode = InstrumentationRegistry.getArguments().getString("cairnEvalLive")?.uppercase(Locale.ROOT)
        assumeTrue("Use cairnEvalLive=local or cairnEvalLive=cloud to make actual Firebase calls.", mode != null)
        val expectedEngine = Engine.valueOf(checkNotNull(mode))
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val target = instrumentation.targetContext
        val firebase = checkNotNull(FirebaseApp.initializeApp(target)) { "Configure Firebase for the app before running live evaluation." }
        installFirebaseAppCheck(firebase)
        val cases = instrumentation.context.assets.open("assistant-cases.json").bufferedReader().use { JSONArray(it.readText()) }
        val report = runBlocking {
            val failures = mutableListOf<String>()
            var attempted = 0
            for (index in 0 until cases.length()) {
                val test = cases.getJSONObject(index)
                if (!test.getBoolean("live") || test.getString("engine") != expectedEngine.name) continue
                attempted++
                val id = test.getString("id")
                val question = test.getString("question")
                val config = Configuration(target.resources.configuration)
                config.setLocale(Locale.forLanguageTag(test.getString("locale")))
                val resources = target.createConfigurationContext(config).resources
                val plan = planAssistant(question, test.getBoolean("cloudAllowed"), test.getBoolean("online"))
                val guide = EmergencyGuide.entries.firstOrNull { it.topic == plan.match.topic }
                if (guide == null || guide.topic.name != test.getString("topic") || plan.match.urgent != test.getBoolean("safetyBanner")) {
                    failures += "$id policy did not select the expected guide and safety banner"
                    continue
                }
                val passages = resources.getStringArray(guide.steps).toList()
                val result = rankPublishedGuidance(question, plan, passages, target.assets)
                if (result?.second != expectedEngine) {
                    failures += "$id: engine ${result?.second ?: "unavailable or invalid response"}"
                    continue
                }
                val answer = (listOf(resources.getString(guide.title)) + result.first.map(passages::get) +
                    resources.getString(guide.source)).joinToString(" ")
                val missing = test.getJSONArray("mustInclude").values().any { !answer.contains(it, ignoreCase = true) }
                val forbidden = test.getJSONArray("mustNotSay").values().any { answer.contains(it, ignoreCase = true) }
                if (missing || forbidden) failures += "$id: answer did not meet its content checks"
            }
            check(attempted > 0) { "The requested engine has no live evaluation cases." }
            "${attempted - failures.size}/$attempted passed ($expectedEngine)" to failures
        }
        Log.i("CairnEval", report.first)
        report.second.forEach { Log.w("CairnEval", it) }
        assertTrue("${report.first}\n${report.second.joinToString("\n")}", report.second.isEmpty())
    }

    private fun JSONArray.values() = (0 until length()).map(::getString)
}
