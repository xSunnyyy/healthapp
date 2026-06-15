package com.sunny.healthapp.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.data.prefs.UserPrefs
import com.sunny.healthapp.data.prefs.UserPrefsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class SettingsState(
    val prefs: UserPrefs = UserPrefs(),
    val availableSources: List<String> = emptyList(),
    val activeSource: String? = null,
    val lastBackfillRunning: Boolean = false,
)

class SettingsViewModel(
    private val app: HealthApp,
) : ViewModel() {

    private val prefsRepo: UserPrefsRepository = app.prefs

    val state: StateFlow<SettingsState> = combine(
        prefsRepo.prefs,
        app.syncManager.status,
    ) { prefs, status ->
        SettingsState(
            prefs = prefs,
            availableSources = app.syncManager.availableSources(),
            activeSource = app.syncManager.primarySource(),
            lastBackfillRunning = status is com.sunny.healthapp.data.sync.SyncStatus.Syncing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    fun setStepsGoal(value: Int) = viewModelScope.launch { prefsRepo.setStepsGoal(value) }
    fun setCaloriesGoal(value: Int) = viewModelScope.launch { prefsRepo.setCaloriesGoal(value) }
    fun setActiveMinutesGoal(value: Int) = viewModelScope.launch { prefsRepo.setActiveMinutesGoal(value) }
    fun setDistanceGoalMiles(value: Float) = viewModelScope.launch { prefsRepo.setDistanceGoalMiles(value) }
    fun setPreferredOrigin(value: String?) = viewModelScope.launch {
        prefsRepo.setPreferredOrigin(value)
        app.triggerManualSync(force = false)
    }

    fun fullRebackfill() = app.triggerManualSync(force = true)

    companion object {
        fun factory(app: HealthApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(app) as T
        }
    }
}
