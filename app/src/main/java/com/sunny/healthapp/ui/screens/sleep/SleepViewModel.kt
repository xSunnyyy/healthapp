package com.sunny.healthapp.ui.screens.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.data.health.HealthRepository
import com.sunny.healthapp.data.sync.SyncStatus
import com.sunny.healthapp.domain.model.SleepSummary
import com.sunny.healthapp.ui.components.Period
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant

data class NightlyPoint(val labelDay: Int, val totalMin: Long, val score: Int)

data class SleepAggregate(
    val sessionCount: Int,
    val avgTotal: Duration,
    val avgTimeInBed: Duration,
    val avgDeep: Duration,
    val avgRem: Duration,
    val avgLight: Duration,
    val avgAwake: Duration,
    val avgScore: Int,
    val avgEfficiency: Int,
    val avgHr: Int?,
    val avgHrv: Double?,
    val avgSpo2: Double?,
    val avgRespiration: Double?,
    val nightly: List<NightlyPoint>,
)

data class SleepState(
    val loading: Boolean = true,
    val period: Period = Period.D,
    val singleSession: SleepSummary? = null,
    val aggregate: SleepAggregate? = null,
)

class SleepViewModel(
    private val app: HealthApp,
) : ViewModel() {
    private val repo: HealthRepository = app.repository

    private val _state = MutableStateFlow(SleepState())
    val state: StateFlow<SleepState> = _state.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> = app.syncManager.status

    private var job: Job? = null

    init {
        load(Period.D)
        viewModelScope.launch {
            var lastSeen: java.time.Instant? = null
            app.syncManager.status.collect { status ->
                if (status is SyncStatus.Done && status.at != lastSeen) {
                    lastSeen = status.at
                    load(_state.value.period)
                }
            }
        }
    }

    fun setPeriod(period: Period) {
        if (period == _state.value.period) return
        load(period)
    }

    fun manualSync() = app.triggerManualSync()

    private fun load(period: Period) {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = SleepState(loading = true, period = period)
            when (period) {
                Period.D -> {
                    val single = runCatching { repo.lastNightSleep() }.getOrNull()
                    _state.value = SleepState(loading = false, period = period, singleSession = single)
                }
                else -> {
                    val now = Instant.now()
                    val from = when (period) {
                        Period.W -> now.minus(Duration.ofDays(7))
                        Period.M -> now.minus(Duration.ofDays(30))
                        Period.SixM -> now.minus(Duration.ofDays(180))
                        Period.Y -> now.minus(Duration.ofDays(365))
                        Period.D -> now
                    }
                    val sessions = runCatching { repo.sleepSessionsRange(from, now) }.getOrDefault(emptyList())
                    val agg = if (sessions.isEmpty()) null else aggregate(sessions)
                    _state.value = SleepState(loading = false, period = period, aggregate = agg)
                }
            }
        }
    }

    private fun aggregate(sessions: List<SleepSummary>): SleepAggregate {
        val totalMinAvg = sessions.map { it.total.toMinutes() }.average().toLong()
        val tibAvg = sessions.map { it.timeInBed.toMinutes() }.average().toLong()
        val deepAvg = sessions.map { it.deep.toMinutes() }.average().toLong()
        val remAvg = sessions.map { it.rem.toMinutes() }.average().toLong()
        val lightAvg = sessions.map { it.light.toMinutes() }.average().toLong()
        val awakeAvg = sessions.map { it.awake.toMinutes() }.average().toLong()
        val scoreAvg = sessions.map { it.score }.average().toInt()
        val effAvg = sessions.map { it.efficiencyPct }.average().toInt()
        val hrAvg = sessions.mapNotNull { it.avgHeartRate }
            .takeIf { it.isNotEmpty() }?.average()?.toInt()
        val hrvAvg = sessions.mapNotNull { it.avgHrv }
            .takeIf { it.isNotEmpty() }?.average()
        val spo2Avg = sessions.mapNotNull { it.avgSpo2 }
            .takeIf { it.isNotEmpty() }?.average()
        val rrAvg = sessions.mapNotNull { it.avgRespiratoryRate }
            .takeIf { it.isNotEmpty() }?.average()

        val nightly = sessions.takeLast(14).map { s ->
            val day = s.end.atZone(java.time.ZoneId.systemDefault()).toLocalDate().dayOfMonth
            NightlyPoint(labelDay = day, totalMin = s.total.toMinutes(), score = s.score)
        }

        return SleepAggregate(
            sessionCount = sessions.size,
            avgTotal = Duration.ofMinutes(totalMinAvg),
            avgTimeInBed = Duration.ofMinutes(tibAvg),
            avgDeep = Duration.ofMinutes(deepAvg),
            avgRem = Duration.ofMinutes(remAvg),
            avgLight = Duration.ofMinutes(lightAvg),
            avgAwake = Duration.ofMinutes(awakeAvg),
            avgScore = scoreAvg,
            avgEfficiency = effAvg,
            avgHr = hrAvg,
            avgHrv = hrvAvg,
            avgSpo2 = spo2Avg,
            avgRespiration = rrAvg,
            nightly = nightly,
        )
    }

    companion object {
        fun factory(app: HealthApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SleepViewModel(app) as T
        }
    }
}
