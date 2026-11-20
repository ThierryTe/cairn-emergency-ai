package com.cairn.emergency.feature.kit

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cairn.emergency.R
import com.cairn.emergency.data.CairnDatabase
import com.cairn.emergency.data.KitCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class KitViewModel(application: Application) : AndroidViewModel(application) {
    private val database by lazy { CairnDatabase.get(application) }
    private var observation: Job? = null

    var plan by mutableStateOf<KitPlan?>(null)
        private set
    var pending by mutableStateOf(setOf<KitSupply>())
        private set
    @get:StringRes
    var issue by mutableStateOf<Int?>(null)
        private set

    init { observe() }

    fun observe() {
        observation?.cancel()
        plan = null
        issue = null
        observation = viewModelScope.launch {
            try {
                combine(database.householdProfileDao().observe(), database.kitCheckDao().observe(), ::buildKitPlan)
                    .collect { plan = it }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                issue = R.string.kit_load_error
            }
        }
    }

    fun setChecked(item: KitItem, checked: Boolean) {
        if (item.supply in pending || plan?.items?.contains(item) != true) return
        pending = pending + item.supply
        issue = null
        viewModelScope.launch {
            try {
                val checks = database.kitCheckDao()
                if (checked) checks.check(KitCheck(item.supply.id, item.planKey))
                else checks.uncheck(item.supply.id, item.planKey)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                issue = R.string.kit_save_error
            } finally {
                pending = pending - item.supply
            }
        }
    }
}
