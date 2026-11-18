package com.cairn.emergency

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cairn.emergency.ui.theme.CairnTheme
import com.cairn.emergency.ai.AiRouter
import com.cairn.emergency.ai.Engine
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import kotlinx.coroutines.launch

private val Navy = Color(0xFF10264C)
private val Blue = Color(0xFF1769AA)
private val PaleBlue = Color(0xFFE9F2FF)
private data class HomeTile(val label: String, val color: Color)
private val homeTiles = listOf(
    HomeTile("👨‍👩‍👧  My Household", Color(0xFFE7F5EE)),
    HomeTile("✚  72-Hour Kit", Color(0xFFFFF0E8)),
    HomeTile("✦  Cairn Assistant", Color(0xFFF1EAFF)),
    HomeTile("ϟ  Crisis Simulator", Color(0xFFE8F2FF)),
)

class MainActivity : ComponentActivity() {
    private var aiMessage by mutableStateOf<String?>(null)
    private var aiBusy by mutableStateOf(false)
    private var screen by mutableStateOf("Home")
    private var householdText by mutableStateOf("")
    private var savedHousehold by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        runCatching {
            FirebaseApp.initializeApp(this)
            if (BuildConfig.DEBUG) {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance(),
                )
            }
        }
        householdText = getPreferences(MODE_PRIVATE).getString("household", "") ?: ""
        savedHousehold = householdText
        setContent {
            CairnTheme {
                if (screen == "Household") {
                    HouseholdScreen(
                        initialText = householdText,
                        savedText = savedHousehold,
                        onBack = { screen = "Home" },
                        onSave = { text ->
                            householdText = text
                            savedHousehold = text
                            getPreferences(MODE_PRIVATE).edit().putString("household", text).apply()
                        },
                    )
                } else {
                    CairnHome(aiMessage, aiBusy, { requestHybridCall() }, { screen = "Household" })
                }
            }
        }
    }

    private fun requestHybridCall() {
        lifecycleScope.launch {
            aiBusy = true
            aiMessage = null
            try {
                val answer = AiRouter.create().generate("Give one concise, safe household preparedness tip.")
                aiMessage = "${if (answer.engine == Engine.LOCAL) "Local · Gemini Nano" else "Cloud · Gemini"}\n${answer.text}"
            } catch (error: Exception) {
                aiMessage = "AI unavailable. Check Firebase setup, App Check, network, and device model."
            } finally {
                aiBusy = false
            }
        }
    }
}

@Composable
private fun CairnHome(aiMessage: String?, aiBusy: Boolean, onTryAi: () -> Unit, onHousehold: () -> Unit) {
    Scaffold(containerColor = Color(0xFFF7F9FD)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("◈  Cairn", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Navy)
                Text("⚙", style = MaterialTheme.typography.headlineSmall, color = Navy)
            }
            Spacer(Modifier.height(20.dp))
            Text("Be prepared.\nStay safe.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Navy)
            Text("Plan, practice, and get help — online or offline.", modifier = Modifier.padding(top = 8.dp), color = Navy)
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(homeTiles) { tile ->
                    Card(colors = CardDefaults.cardColors(containerColor = tile.color), shape = RoundedCornerShape(20.dp), onClick = { if (tile.label.contains("Household")) onHousehold() }) {
                        Text(tile.label, Modifier.padding(18.dp), color = Navy, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Ready when the network isn't", color = Blue, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onTryAi, enabled = !aiBusy, modifier = Modifier.fillMaxWidth()) {
                Text(if (aiBusy) "Checking hybrid AI…" else "Try hybrid AI")
            }
            aiMessage?.let { Text(it, Modifier.padding(top = 8.dp), color = Navy) }
            Spacer(Modifier.weight(1f))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(30.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD93636))) {
                Text("☎  Emergency SOS", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Text("Cairn does not replace emergency services.", modifier = Modifier.fillMaxWidth().background(PaleBlue, RoundedCornerShape(12.dp)).padding(12.dp), color = Navy)
        }
    }
}

@Composable
private fun HouseholdScreen(initialText: String, savedText: String, onBack: () -> Unit, onSave: (String) -> Unit) {
    var draft by androidx.compose.runtime.remember(initialText) { mutableStateOf(initialText) }
    Scaffold(containerColor = Color(0xFFF7F9FD)) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onBack) { Text("Back") }
                Text("My Household", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Navy, modifier = Modifier.padding(top = 8.dp))
            }
            Text("Describe who you are preparing for. Review this information before saving.", color = Navy)
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Household details") },
                placeholder = { Text("2 adults, one baby, insulin, and a dog") },
                minLines = 5,
            )
            if (savedText.isNotBlank()) {
                Text("Saved on this device: $savedText", color = Blue)
            }
            Button(onClick = { onSave(draft) }, enabled = draft.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Save household")
            }
            Text("Your household details stay on this device.", color = Navy)
        }
    }
}
