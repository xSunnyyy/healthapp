package com.sunny.healthapp.ui.screens.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.data.health.HealthRepository
import com.sunny.healthapp.data.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TrendPoint(val date: LocalDate, val value: Double)

data class TrendState(
    val loading: Boolean = true,
    val title: String,
    val unit: String,
    val points: List<TrendPoint> = emptyList(),
    val latest: Double? = null,
    val baseline: Double? = null,
    val low: Double? = null,
    val high: Double? = null,
)

class HrvTrendViewModel(private val app: HealthApp) : ViewModel() {
    private val repo: HealthRepository = app.repository

    private val _state = MutableStateFlow(TrendState(title = "HRV", unit = "ms"))
    val state: StateFlow<TrendState> = _state.asStateFlow()
    val syncStatus: StateFlow<SyncStatus> = app.syncManager.status

    init {
        load()
        viewModelScope.launch {
            var lastSeen: Instant? = null
            app.syncManager.status.collect { status ->
                if (status is SyncStatus.Done && status.at != lastSeen) {
                    lastSeen = status.at
                    load()
                }
            }
        }
    }

    fun manualSync() = app.triggerManualSync()

    private fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val today = LocalDate.now()
            val zone = ZoneId.systemDefault()
            // 30 nights of HRV from sleep sessions
            val sessions = (0..29).mapNotNull { offset ->
                val date = today.minusDays(offset.toLong())
                runCatching { repo.sleepOnDate(date) }.getOrNull()?.let { s ->
                    s.avgHrv?.let { hrv -> TrendPoint(date, hrv) }
                }
            }.sortedBy { it.date }

            val values = sessions.map { it.value }
            _state.value = TrendState(
                loading = false,
                title = "HRV",
                unit = "ms",
                points = sessions,
                latest = values.lastOrNull(),
                baseline = if (values.isNotEmpty()) values.average() else null,
                low = values.minOrNull(),
                high = values.maxOrNull(),
            )
        }
    }

    companion object {
        fun factory(app: HealthApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HrvTrendViewModel(app) as T
        }
    }
}
