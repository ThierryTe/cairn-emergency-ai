package com.cairn.emergency.ui.settings

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cairn.emergency.R
import com.cairn.emergency.ui.home.CairnIcon
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private val Context.accessibilityPreferences by preferencesDataStore("cairn_accessibility")

internal enum class AccessibilityOption(
    key: String,
    @StringRes val title: Int,
    @StringRes val subtitle: Int?,
    val icon: CairnIcon,
    val default: Boolean = false,
) {
    LargeText("large_text", R.string.settings_large_text, null, CairnIcon.TextSize),
    HighContrast("high_contrast", R.string.settings_high_contrast, null, CairnIcon.Contrast),
    ReadAloud("read_aloud", R.string.settings_read_aloud, R.string.settings_speech, CairnIcon.Speaker, true),
    Simplified("simplified", R.string.settings_simplified, R.string.settings_simplified_description, CairnIcon.Documents),
    Vibration("vibration", R.string.settings_vibration, R.string.settings_vibration_description, CairnIcon.Vibration);

    val storageKey = booleanPreferencesKey(key)
}

internal class AccessibilityViewModel(application: Application) : AndroidViewModel(application) {
    private val store = application.accessibilityPreferences
    private var values by mutableStateOf<Preferences?>(null)

    val loaded get() = values != null

    var storageIssue by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            store.data.catch { error ->
                if (error !is IOException) throw error
                storageIssue = true
            }.collect { values = it }
        }
    }

    fun enabled(option: AccessibilityOption) = values?.get(option.storageKey) ?: option.default

    fun set(option: AccessibilityOption, enabled: Boolean) {
        viewModelScope.launch {
            try {
                values = store.edit { it[option.storageKey] = enabled }
                storageIssue = false
            } catch (_: IOException) {
                storageIssue = true
            }
        }
    }
}
