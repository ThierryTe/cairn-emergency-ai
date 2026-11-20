package com.cairn.emergency.feature.household

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cairn.emergency.R
import com.cairn.emergency.ui.settings.CairnLanguage
import com.cairn.emergency.ai.AiRouter
import com.cairn.emergency.ai.Engine
import com.cairn.emergency.data.CairnDatabase
import com.cairn.emergency.data.HouseholdProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal data class HouseholdState(
    val description: String = "",
    val saved: HouseholdProfile? = null,
    val draft: HouseholdProfile? = null,
    val editing: Boolean = false,
    val aiDraft: Boolean = false,
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val generating: Boolean = false,
    val saving: Boolean = false,
    val message: Int? = null,
) {
    val busy get() = loading || generating || saving
}

internal class HouseholdViewModel(application: Application) : AndroidViewModel(application) {
    private val profiles by lazy { CairnDatabase.get(application).householdProfileDao() }

    var state by mutableStateOf(HouseholdState())
        private set

    init { load() }

    fun load() {
        state = state.copy(loading = true, loadFailed = false, message = null)
        viewModelScope.launch {
            try {
                val profile = profiles.get() ?: importLegacyDescription()
                state = HouseholdState(description = profile?.originalDescription.orEmpty(), saved = profile, loading = false)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                state = state.copy(loading = false, loadFailed = true, message = R.string.household_load_failed)
            }
        }
    }

    private suspend fun importLegacyDescription(): HouseholdProfile? {
        val preferences = getApplication<Application>().getSharedPreferences("MainActivity", Context.MODE_PRIVATE)
        val original = preferences.getString("household", null)?.takeIf(String::isNotBlank) ?: return null
        val profile = HouseholdProfile(originalDescription = original)
        profiles.save(profile)
        preferences.edit().remove("household").apply()
        return profile
    }

    fun setDescription(description: String) {
        if (state.busy) return
        state = state.copy(description = description.take(MaximumHouseholdDescription), message = null)
    }

    fun editProfile() {
        if (state.busy || state.loadFailed) return
        state = state.copy(draft = state.draft ?: state.saved ?: HouseholdProfile(), editing = true, message = null)
    }

    fun updateDraft(profile: HouseholdProfile) {
        if (state.busy || !state.editing || state.draft == null) return
        state = state.copy(draft = profile, message = null)
    }

    fun discardDraft() {
        if (state.busy) return
        state = state.copy(draft = null, editing = false, aiDraft = false, message = null)
    }

    fun suggestProfile(language: CairnLanguage) {
        val description = state.description.trim()
        if (state.busy || state.loadFailed || state.draft != null || description.isEmpty()) return
        state = state.copy(generating = true, message = null)
        viewModelScope.launch {
            try {
                val router = AiRouter.createLocal()
                if (router == null) {
                    state = state.copy(message = R.string.household_ai_unavailable)
                    return@launch
                }

                val instructions = withContext(Dispatchers.IO) {
                    getApplication<Application>().assets.open("prompts/household-v1.txt").bufferedReader().use { it.readText() }
                }
                val prompt = "$instructions\nWrite all text fields in ${language.promptName}.\nHousehold description: ${JSONObject.quote(description)}"
                var draft = generateDraft(router, prompt, description)
                if (draft == null) {
                    draft = generateDraft(router, "$prompt\nCheck the field types and return only the JSON object.", description)
                }
                state = if (draft == null) {
                    state.copy(message = R.string.household_ai_invalid)
                } else {
                    state.copy(draft = draft, aiDraft = true, message = R.string.household_review)
                }
            } catch (_: TimeoutCancellationException) {
                state = state.copy(message = R.string.household_ai_timeout)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                state = state.copy(message = R.string.household_ai_unavailable)
            } finally {
                state = state.copy(generating = false)
            }
        }
    }

    private suspend fun generateDraft(router: AiRouter, prompt: String, description: String): HouseholdProfile? {
        val output = router.generate(prompt)
        return if (output.engine == Engine.LOCAL) parseHouseholdDraft(output.text, description) else null
    }

    fun save() {
        val draft = state.draft ?: return
        if (state.busy || state.loadFailed) return
        val profile = draft.copy(originalDescription = state.description.trim())
        state = state.copy(saving = true, message = null)
        viewModelScope.launch {
            try {
                profiles.save(profile)
                state = state.copy(
                    saved = profile,
                    draft = null,
                    editing = false,
                    aiDraft = false,
                    message = R.string.household_saved,
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                state = state.copy(message = R.string.household_save_failed)
            } finally {
                state = state.copy(saving = false)
            }
        }
    }
}
