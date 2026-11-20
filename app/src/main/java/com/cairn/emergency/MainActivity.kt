package com.cairn.emergency

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.ViewModelProvider
import android.view.HapticFeedbackConstants
import com.cairn.emergency.ai.installFirebaseAppCheck
import com.cairn.emergency.feature.assistant.AssistantScreen
import com.cairn.emergency.feature.assistant.AssistantViewModel
import com.cairn.emergency.feature.household.HouseholdScreen
import com.cairn.emergency.feature.household.HouseholdViewModel
import com.cairn.emergency.feature.kit.KitScreen
import com.cairn.emergency.feature.kit.KitViewModel
import com.cairn.emergency.feature.simulator.SimulatorScreen
import com.cairn.emergency.feature.simulator.SimulatorViewModel
import com.cairn.emergency.ui.home.CairnDestination
import com.cairn.emergency.ui.home.CairnHome
import com.cairn.emergency.ui.settings.AccessibilityOption
import com.cairn.emergency.ui.settings.AccessibilityScreen
import com.cairn.emergency.ui.settings.AccessibilityViewModel
import com.cairn.emergency.ui.settings.CairnLocales
import com.cairn.emergency.ui.sos.EmergencySosScreen
import com.cairn.emergency.ui.theme.CairnTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    private val navigation = mutableStateOf(listOf(CairnDestination.Home))
    private val household by lazy { ViewModelProvider(this)[HouseholdViewModel::class.java] }
    private val accessibility by lazy { ViewModelProvider(this)[AccessibilityViewModel::class.java] }
    private val assistant by lazy { ViewModelProvider(this)[AssistantViewModel::class.java] }
    private val simulator by lazy { ViewModelProvider(this)[SimulatorViewModel::class.java] }
    private val kit by lazy { ViewModelProvider(this)[KitViewModel::class.java] }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(CairnLocales.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val restored = savedInstanceState?.getStringArrayList("navigation")?.mapNotNull(::destinationNamed)
        val previous = savedInstanceState?.getString("screen")?.let(::destinationNamed)
        navigation.value = restored?.takeIf { it.firstOrNull() == CairnDestination.Home }
            ?: when {
                previous == CairnDestination.Household && savedInstanceState.getBoolean("householdOpenedFromKit") ->
                    listOf(CairnDestination.Home, CairnDestination.Kit, previous)
                previous == null || previous == CairnDestination.Home -> listOf(CairnDestination.Home)
                else -> listOf(CairnDestination.Home, previous)
            }
        CairnLocales.migrateLegacySelection(this)
        val language = CairnLocales.current(this)
        FirebaseApp.initializeApp(this)?.let(::installFirebaseAppCheck)
        setContent {
            val stack = navigation.value
            val destination = stack.last()
            val screenState = rememberSaveableStateHolder()
            SideEffect {
                val clear = Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(clear),
                    navigationBarStyle = SystemBarStyle.light(clear, Color.BLACK),
                )
            }
            BackHandler(enabled = stack.size > 1, onBack = ::navigateBack)
            val readAloud = accessibility.enabled(AccessibilityOption.ReadAloud)
            val simplified = accessibility.enabled(AccessibilityOption.Simplified)
            CairnTheme(
                darkTheme = false,
                dynamicColor = false,
                largeText = accessibility.enabled(AccessibilityOption.LargeText),
                highContrast = accessibility.enabled(AccessibilityOption.HighContrast),
            ) {
                screenState.SaveableStateProvider(destination.name) {
                    when (destination) {
                        CairnDestination.Home -> CairnHome(
                            onHousehold = { navigateTo(CairnDestination.Household) },
                            onKit = { navigateTo(CairnDestination.Kit) },
                            onAssistant = { navigateTo(CairnDestination.Assistant) },
                            onSimulator = { navigateTo(CairnDestination.Simulator) },
                            onSos = { navigateTo(CairnDestination.Sos) },
                            onTalkSos = { navigateTo(CairnDestination.VoiceSos) },
                            onSettings = { navigateTo(CairnDestination.Settings) },
                            kitProgress = kit.plan?.readyPercent,
                            kitCountsIncomplete = kit.plan?.householdCountsIncomplete == true,
                        )
                        CairnDestination.Household -> HouseholdScreen(
                            household, lifecycle, language, { navigateTo(CairnDestination.Kit) }, ::navigateBack,
                        )
                        CairnDestination.Kit -> KitScreen(
                            kit, { navigateTo(CairnDestination.Household) }, ::navigateBack,
                            onCheckFeedback = {
                                if (accessibility.enabled(AccessibilityOption.Vibration)) {
                                    window.decorView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                }
                            },
                        )
                        CairnDestination.Assistant -> AssistantScreen(
                            assistant, lifecycle, language, readAloud, simplified, { navigateTo(CairnDestination.Sos) }, ::navigateBack,
                        )
                        CairnDestination.Simulator -> SimulatorScreen(simulator, lifecycle, language, readAloud, ::navigateBack)
                        CairnDestination.Sos, CairnDestination.VoiceSos -> EmergencySosScreen(
                            household.state.saved, language, lifecycle, readAloud, simplified, ::navigateBack,
                            startWithVoice = destination == CairnDestination.VoiceSos,
                        )
                        CairnDestination.Settings -> AccessibilityScreen(language, accessibility, { CairnLocales.select(this, it) }, ::navigateBack)
                    }
                }
            }
        }
    }

    private fun destinationNamed(name: String): CairnDestination? = CairnDestination.entries.firstOrNull { it.name == name }

    private fun navigateTo(destination: CairnDestination) {
        val stack = navigation.value
        val index = stack.indexOf(destination)
        navigation.value = if (index >= 0) stack.take(index + 1) else stack + destination
    }

    private fun navigateBack() {
        if (navigation.value.size > 1) navigation.value = navigation.value.dropLast(1)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("navigation", ArrayList(navigation.value.map { it.name }))
        super.onSaveInstanceState(outState)
    }
}
