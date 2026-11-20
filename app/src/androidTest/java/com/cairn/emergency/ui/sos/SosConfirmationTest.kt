package com.cairn.emergency.ui.sos

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.cairn.emergency.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SosConfirmationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun cancelKeepsDialerFromOpening() {
        var confirmations = 0
        showConfirmation(SosCommunication.Dialer) { confirmations++ }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.voice_sos_confirm_dial)).assertExists()
        compose.runOnIdle { assertEquals(0, confirmations) }
        compose.onNodeWithText(context.getString(R.string.household_cancel)).performClick()
        compose.runOnIdle { assertEquals(0, confirmations) }
    }

    @Test
    fun messagesRequireTheConfirmButton() {
        var confirmations = 0
        showConfirmation(SosCommunication.Messages("Test draft")) { confirmations++ }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.voice_sos_confirm_sms)).assertExists()
        compose.runOnIdle { assertEquals(0, confirmations) }
        compose.onNodeWithText(context.getString(R.string.sos_open_messages)).performClick()
        compose.runOnIdle { assertEquals(1, confirmations) }
    }

    private fun showConfirmation(action: SosCommunication, confirmed: () -> Unit) {
        compose.setContent {
            var visible by remember { mutableStateOf(true) }
            if (visible) {
                ConfirmSosCommunication(action, null, onDismiss = { visible = false }) {
                    visible = false
                    confirmed()
                }
            }
        }
    }
}
