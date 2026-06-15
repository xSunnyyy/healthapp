package com.sunny.healthapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.data.health.HealthRepository
import com.sunny.healthapp.data.sync.SyncStatus
import com.sunny.healthapp.domain.model.DailySummary
import com.sunny.healthapp.domain.model.ReadinessSummary
import com.sunny.healthapp.domain.model.SleepSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeState(
    val loading: Boolean = true,
    val date: LocalDate = LocalDate.now(),
    val daily: DailySummary? = null,
    val previousDaily: DailySummary? = null,
    val sleep: SleepSummary? = null,
    val readiness: ReadinessSummary? = null,
    val weeklySteps: List<Pair<LocalDate, Long>> = emptyList(),
)

class HomeViewModel(
    private val app: HealthApp,
) : ViewModel() {
    private val repo: HealthRepository = app.repository

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> = app.syncManager.status

    private var currentJob: Job? = null

    init {
        load(LocalDate.now())
        // Auto-reload whenever a sync completes so the UI catches up to Room.
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

    fun goToDate(date: LocalDate) {
        if (date == _state.value.date) return
        load(date)
    }

    fun next() {
        val next = _state.value.date.plusDays(1)
        if (!next.isAfter(LocalDate.now())) load(next)
    }

    fun previous() = load(_state.value.date.minusDays(1))

    fun manualSync() = app.triggerManualSync()

    private fun load(date: LocalDate) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, date = date)
            val daily = runCatching { repo.dailySummary(date) }.getOrNull()
            val previous = runCatching { repo.dailySummary(date.minusDays(1)) }.getOrNull()
            val sleep = runCatching {
                if (date == LocalDate.now()) repo.lastNightSleep() else repo.sleepOnDate(date)
            }.getOrNull()
            val readiness = runCatching {
                if (date == LocalDate.now()) repo.readiness() else null
            }.getOrNull()
            val weekly = (6 downTo 0).map { offset ->
                val d = date.minusDays(offset.toLong())
                val steps = runCatching { repo.dailySummary(d).steps }.getOrDefault(0L)
                d to steps
            }
            _state.value = HomeState(
                loading = false,
                date = date,
                daily = daily,
                previousDaily = previous,
                sleep = sleep,
                readiness = readiness,
                weeklySteps = weekly,
            )
        }
    }

    companion object {
        fun factory(app: HealthApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(app) as T
        }
    }
}
