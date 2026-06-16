package com.sunny.healthapp.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.data.prefs.UserPrefs
import com.sunny.healthapp.data.prefs.UserPrefsRepository
import com.sunny.healthapp.data.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SettingsState(
    val prefs: UserPrefs = UserPrefs(),
    val availableSources: List<String> = emptyList(),
    val activeSource: String? = null,
    val lastBackfillRunning: Boolean = false,
    val diagnostics: Diagnostics = Diagnostics(),
)

data class Diagnostics(
    val lastSyncAt: Instant? = null,
    val lastSyncMessage: String? = null,
    val latestHrTime: Instant? = null,
    val latestHrBpm: Int? = null,
    val todayHrSampleCount: Int = 0,
    val todaySteps: Long = 0L,
)

class SettingsViewModel(
    private val app: HealthApp,
) : ViewModel() {

    private val prefsRepo: UserPrefsRepository = app.prefs

    private val _diagnostics = MutableStateFlow(Diagnostics())

    init {
        // Refresh diagnostics any time the sync state changes (so 'today's HR
        // sample count' and 'latest HR' update after a successful resync).
        viewModelScope.launch {
            app.syncManager.status.collect { status ->
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now()
                val dayStart = today.atStartOfDay(zone).toInstant()
                val now = Instant.now()
                val hr = runCatching { app.repository.hrSamples(dayStart, now) }
                    .getOrDefault(emptyList())
                val daily = runCatching { app.repository.dailySummary(today) }.getOrNull()
                val latest = hr.maxByOrNull { it.time }
                _diagnostics.update {
                    Diagnostics(
                        lastSyncAt = (status as? SyncStatus.Done)?.at,
                        lastSyncMessage = when (status) {
                            is SyncStatus.Error -> status.message
                            is SyncStatus.Syncing -> status.message
                            else -> null
                        },
                        latestHrTime = latest?.time,
                        latestHrBpm = latest?.bpm,
                        todayHrSampleCount = hr.size,
                        todaySteps = daily?.steps ?: 0L,
                    )
                }
            }
        }
    }

    val state: StateFlow<SettingsState> = combine(
        prefsRepo.prefs,
        app.syncManager.status,
        _diagnostics,
    ) { prefs, status, diag ->
        SettingsState(
            prefs = prefs,
            availableSources = app.syncManager.availableSources(),
            activeSource = app.syncManager.primarySource(),
            lastBackfillRunning = status is SyncStatus.Syncing,
            diagnostics = diag,
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

    fun setPhoneFillEnabled(value: Boolean) = viewModelScope.launch {
        prefsRepo.setPhoneFillEnabled(value)
        app.triggerManualSync(force = true)
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
