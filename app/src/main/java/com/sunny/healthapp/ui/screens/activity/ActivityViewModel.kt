package com.sunny.healthapp.ui.screens.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.data.health.HealthRepository
import com.sunny.healthapp.data.sync.SyncStatus
import com.sunny.healthapp.domain.model.DailySummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ActivityState(
    val loading: Boolean = true,
    val date: LocalDate = LocalDate.now(),
    val selected: DailySummary? = null,
    val previousDay: DailySummary? = null,
    val recent: List<DailySummary> = emptyList(),
)

class ActivityViewModel(
    private val app: HealthApp,
) : ViewModel() {
    private val repo: HealthRepository = app.repository

    private val _state = MutableStateFlow(ActivityState())
    val state: StateFlow<ActivityState> = _state.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> = app.syncManager.status

    private var job: Job? = null

    init {
        load(LocalDate.now())
        viewModelScope.launch {
            var wasSyncing = false
            app.syncManager.status.collect { status ->
                if (status is SyncStatus.Syncing) wasSyncing = true
                else if (wasSyncing && status is SyncStatus.Done) {
                    wasSyncing = false
                    load(_state.value.date)
                }
            }
        }
    }

    fun setDate(date: LocalDate) {
        if (date == _state.value.date) return
        load(date)
    }

    fun manualSync() = app.triggerManualSync()

    private fun load(date: LocalDate) {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, date = date)
            val selected = runCatching { repo.dailySummary(date) }.getOrNull()
            val prev = runCatching { repo.dailySummary(date.minusDays(1)) }.getOrNull()
            // Past 7 days ending at the selected date so the weekly chart shifts
            // with the selection.
            val recent = (6 downTo 0).mapNotNull { offset ->
                runCatching { repo.dailySummary(date.minusDays(offset.toLong())) }.getOrNull()
            }
            _state.value = ActivityState(
                loading = false,
                date = date,
                selected = selected,
                previousDay = prev,
                recent = recent,
            )
        }
    }

    companion object {
        fun factory(app: HealthApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ActivityViewModel(app) as T
        }
    }
}
