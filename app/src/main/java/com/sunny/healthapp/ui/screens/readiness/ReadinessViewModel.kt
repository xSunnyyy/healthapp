package com.sunny.healthapp.ui.screens.readiness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.data.health.HealthRepository
import com.sunny.healthapp.data.sync.SyncStatus
import com.sunny.healthapp.domain.model.ReadinessSummary
import com.sunny.healthapp.ui.components.Period
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class ReadinessState(
    val loading: Boolean = true,
    val period: Period = Period.D,
    val readiness: ReadinessSummary? = null,
    val latestHr: Int? = null,
    val latestHrTime: Instant? = null,
    val avgHr: Int? = null,
    val restingHr: Int? = null,
    val minHr: Int? = null,
    val maxHr: Int? = null,
    val maxHrTime: Instant? = null,
    val chartValues: List<Float> = emptyList(),
    val chartLabels: List<String> = emptyList(),
    // zone percents
    val pctNormal: Int = 0,
    val pctElevated: Int = 0,
    val pctHigh: Int = 0,
)

class ReadinessViewModel(
    private val app: HealthApp,
) : ViewModel() {
    private val repo: HealthRepository = app.repository

    private val _state = MutableStateFlow(ReadinessState())
    val state: StateFlow<ReadinessState> = _state.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> = app.syncManager.status

    private var job: Job? = null

    init {
        load(Period.D)
        viewModelScope.launch {
            var wasSyncing = false
            app.syncManager.status.collect { status ->
                if (status is SyncStatus.Syncing) wasSyncing = true
                else if (wasSyncing && status is SyncStatus.Done) {
                    wasSyncing = false
                    load(_state.value.period)
                }
            }
        }
    }

    fun setPeriod(period: Period) {
        if (period == _state.value.period) return
        load(period)
    }

    private fun load(period: Period) {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, period = period)
            val zone = ZoneId.systemDefault()
            val now = Instant.now()
            val from = when (period) {
                Period.D -> LocalDate.now().atStartOfDay(zone).toInstant()
                Period.W -> now.minus(Duration.ofDays(7))
                Period.M -> now.minus(Duration.ofDays(30))
                Period.SixM -> now.minus(Duration.ofDays(180))
                Period.Y -> now.minus(Duration.ofDays(365))
            }
            val samples = runCatching { repo.hrSamples(from, now) }.getOrDefault(emptyList())
            val readiness = runCatching { repo.readiness() }.getOrNull()

            val bpms = samples.map { it.bpm }
            val latest = samples.maxByOrNull { it.time }
            val max = samples.maxByOrNull { it.bpm }
            val restingHr = repo.dailySummary(LocalDate.now()).restingHeartRate

            val totalCount = bpms.size.coerceAtLeast(1)
            val normalCount = bpms.count { it in 60..99 }
            val elevatedCount = bpms.count { it in 100..129 }
            val highCount = bpms.count { it >= 130 }
            val pctN = (normalCount * 100) / totalCount
            val pctE = (elevatedCount * 100) / totalCount
            val pctH = (highCount * 100) / totalCount

            // Downsample for chart
            val (chartVals, chartLabels) = downsample(samples, period, zone)

            _state.value = ReadinessState(
                loading = false,
                period = period,
                readiness = readiness,
                latestHr = latest?.bpm,
                latestHrTime = latest?.time,
                avgHr = bpms.takeIf { it.isNotEmpty() }?.average()?.toInt(),
                restingHr = restingHr,
                minHr = bpms.minOrNull(),
                maxHr = max?.bpm,
                maxHrTime = max?.time,
                chartValues = chartVals,
                chartLabels = chartLabels,
                pctNormal = pctN,
                pctElevated = pctE,
                pctHigh = pctH,
            )
        }
    }

    private fun downsample(
        samples: List<HealthRepository.HrSample>,
        period: Period,
        zone: ZoneId,
    ): Pair<List<Float>, List<String>> {
        if (samples.isEmpty()) return emptyList<Float>() to listOf("—")
        return when (period) {
            Period.D -> bucketByMinutes(samples, 30, zone) { it.format("h a") } to
                listOf("6AM", "12PM", "3PM", "6PM", "Now")
            Period.W -> bucketByDays(samples, 1, zone) to
                listOf("M", "T", "W", "T", "F", "S", "S")
            Period.M -> bucketByDays(samples, 1, zone) to listOf("Wk 1", "Wk 2", "Wk 3", "Wk 4")
            Period.SixM -> bucketByDays(samples, 7, zone) to
                listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
            Period.Y -> bucketByDays(samples, 30, zone) to listOf("Q1", "Q2", "Q3", "Q4")
        }
    }

    private fun bucketByMinutes(
        samples: List<HealthRepository.HrSample>,
        bucketMin: Int,
        zone: ZoneId,
        @Suppress("UNUSED_PARAMETER") fmt: (Instant) -> String,
    ): List<Float> {
        if (samples.isEmpty()) return emptyList()
        val first = samples.first().time
        val groups = samples.groupBy { sample ->
            Duration.between(first, sample.time).toMinutes() / bucketMin
        }
        return groups.toSortedMap().map { (_, list) ->
            list.map { it.bpm }.average().toFloat()
        }
    }

    private fun bucketByDays(
        samples: List<HealthRepository.HrSample>,
        bucketDays: Int,
        zone: ZoneId,
    ): List<Float> {
        if (samples.isEmpty()) return emptyList()
        val first = samples.first().time.atZone(zone).toLocalDate()
        val groups = samples.groupBy { sample ->
            Duration.between(
                first.atStartOfDay(zone).toInstant(),
                sample.time,
            ).toDays() / bucketDays
        }
        return groups.toSortedMap().map { (_, list) ->
            list.map { it.bpm }.average().toFloat()
        }
    }

    private fun Instant.format(pattern: String): String =
        java.time.format.DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault()).format(this)

    fun manualSync() = app.triggerManualSync()

    companion object {
        fun factory(app: HealthApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ReadinessViewModel(app) as T
        }
    }
}
