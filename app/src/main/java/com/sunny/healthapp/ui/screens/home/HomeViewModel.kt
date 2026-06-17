package com.sunny.healthapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.data.health.HealthRepository
import com.sunny.healthapp.data.sync.SyncStatus
import com.sunny.healthapp.domain.AnomalyDetector
import com.sunny.healthapp.domain.BodyBatteryCalculator
import com.sunny.healthapp.domain.model.AnomalyInsight
import com.sunny.healthapp.domain.model.BodyBatterySummary
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
    val bodyBattery: BodyBatterySummary? = null,
    val weeklySteps: List<Pair<LocalDate, Long>> = emptyList(),
    val weeklyCalories: List<Pair<LocalDate, Double>> = emptyList(),
    val weeklySleepMin: List<Pair<LocalDate, Long>> = emptyList(),
    val insights: WeeklyInsights? = null,
    val anomalies: List<AnomalyInsight> = emptyList(),
)

data class WeeklyInsights(
    val avgSleepMinThisWeek: Long,
    val avgSleepMinLastWeek: Long?,
    val sleepDeltaMin: Long?,
    val stepsGoalHitDays: Int,
    val stepsGoal: Int,
    val rhrAvgThisWeek: Int?,
    val rhrAvgLastWeek: Int?,
    val rhrDelta: Int?,
    val avgStepsThisWeek: Int,
    val avgStepsLastWeek: Int?,
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
        // We reload on every NEW Done timestamp (deduped) so this also fires
        // when a sync had already finished before this ViewModel subscribed —
        // which was the cause of the 'shows 0 until manual re-sync' bug.
        viewModelScope.launch {
            var lastSeen: java.time.Instant? = null
            app.syncManager.status.collect { status ->
                if (status is SyncStatus.Done && status.at != lastSeen) {
                    lastSeen = status.at
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

    private suspend fun computeAnomalies(date: LocalDate): List<AnomalyInsight> {
        // Pull 30 days of history (excluding 'date' itself) for the rolling baseline.
        val historyStart = date.minusDays(30)
        val historyDays = (1..30).map { date.minusDays(it.toLong()) }
        val history = historyDays.map { runCatching { repo.dailySummary(it) }.getOrNull() }

        val rhrHist = history.mapNotNull { it?.restingHeartRate }
        val stepsHist = history.mapNotNull { it?.steps?.takeIf { s -> s > 0 } }

        val sleepHist = historyDays.mapNotNull { d ->
            runCatching { repo.sleepOnDate(d) }.getOrNull()?.total?.toMinutes()?.takeIf { it > 0 }
        }
        val hrvHist = historyDays.mapNotNull { d ->
            runCatching { repo.sleepOnDate(d) }.getOrNull()?.avgHrv
        }

        val today = runCatching { repo.dailySummary(date) }.getOrNull()
        val sleepToday = runCatching { repo.sleepOnDate(date) }.getOrNull()

        return AnomalyDetector.detect(
            AnomalyDetector.Inputs(
                rhrToday = today?.restingHeartRate,
                rhrHistory = rhrHist,
                hrvToday = sleepToday?.avgHrv,
                hrvHistory = hrvHist,
                sleepMinToday = sleepToday?.total?.toMinutes(),
                sleepMinHistory = sleepHist,
                stepsToday = today?.steps,
                stepsHistory = stepsHist,
            )
        )
    }

    private suspend fun computeBodyBattery(
        date: LocalDate,
        sleep: SleepSummary?,
    ): BodyBatterySummary {
        val zone = java.time.ZoneId.systemDefault()
        val now = java.time.Instant.now()
        val dayStart = date.atStartOfDay(zone).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()

        // HR samples for the calculator window (from wake or day-start to now)
        val wake = sleep?.end ?: dayStart
        val windowEnd = minOf(now, dayEnd)
        val hr = if (wake < windowEnd) {
            repo.hrSamples(wake, windowEnd).map { it.time to it.bpm }
        } else emptyList()

        // Active calories per hour for the day so far. Roughly: scale today's total
        // by the share of HR samples falling in each hour.
        val daily = repo.dailySummary(date)
        val totalKcal = daily.activeCalories
        val byHour = mutableMapOf<Int, Double>()
        if (hr.isNotEmpty() && totalKcal > 0) {
            val perHourCount = hr.groupingBy { it.first.atZone(zone).hour }.eachCount()
            val totalSamples = perHourCount.values.sum().coerceAtLeast(1)
            perHourCount.forEach { (h, count) ->
                byHour[h] = totalKcal * (count.toDouble() / totalSamples)
            }
        }

        // Per-minute steps (epochMinute -> count) so we can detect 'quiet minutes'.
        val stepsByMinute = runCatching {
            if (wake < windowEnd) repo.stepsByMinute(wake, windowEnd) else emptyMap()
        }.getOrDefault(emptyMap())

        // Daytime naps between wake and now contribute to recharge.
        val naps = runCatching {
            if (wake < windowEnd) repo.napsBetween(wake, windowEnd) else emptyList()
        }.getOrDefault(emptyList())

        return BodyBatteryCalculator.compute(
            BodyBatteryCalculator.Inputs(
                now = now,
                zone = zone,
                lastNightSleep = sleep,
                hrvBaselineMs = runCatching { repo.hrvBaseline() }.getOrNull(),
                rhrBaselineBpm = runCatching { repo.rhrBaseline() }.getOrNull(),
                avgBedtime = runCatching { repo.bedtimeAverage(zone) }.getOrNull(),
                hrSamples = hr,
                activeKcalByHour = byHour,
                stepsByMinute = stepsByMinute,
                napsToday = naps,
            )
        )
    }

    private suspend fun computeInsights(date: LocalDate): WeeklyInsights {
        val prefs = app.prefs.current()
        val thisWeekStart = date.minusDays(6)
        val lastWeekStart = date.minusDays(13)
        val lastWeekEnd = date.minusDays(7)

        val thisWeekDays = (0..6).map { repo.dailySummary(thisWeekStart.plusDays(it.toLong())) }
        val lastWeekDays = (0..6).map { repo.dailySummary(lastWeekStart.plusDays(it.toLong())) }

        val thisWeekSleep = (0..6).mapNotNull { offset ->
            repo.sleepOnDate(thisWeekStart.plusDays(offset.toLong()))?.total?.toMinutes()
        }
        val lastWeekSleep = (0..6).mapNotNull { offset ->
            repo.sleepOnDate(lastWeekStart.plusDays(offset.toLong()))?.total?.toMinutes()
        }
        val avgSleepThis = if (thisWeekSleep.isNotEmpty()) thisWeekSleep.average().toLong() else 0L
        val avgSleepLast = if (lastWeekSleep.isNotEmpty()) lastWeekSleep.average().toLong() else null
        val sleepDelta = avgSleepLast?.let { avgSleepThis - it }

        val goalsHit = thisWeekDays.count { it.steps >= prefs.stepsGoal }

        val rhrThis = thisWeekDays.mapNotNull { it.restingHeartRate }
            .takeIf { it.isNotEmpty() }?.average()?.toInt()
        val rhrLast = lastWeekDays.mapNotNull { it.restingHeartRate }
            .takeIf { it.isNotEmpty() }?.average()?.toInt()
        val rhrDelta = if (rhrThis != null && rhrLast != null) rhrThis - rhrLast else null

        val avgStepsThis = thisWeekDays.map { it.steps }.average().toInt()
        val avgStepsLast = lastWeekDays.map { it.steps }.takeIf { it.any { s -> s > 0 } }
            ?.average()?.toInt()

        return WeeklyInsights(
            avgSleepMinThisWeek = avgSleepThis,
            avgSleepMinLastWeek = avgSleepLast,
            sleepDeltaMin = sleepDelta,
            stepsGoalHitDays = goalsHit,
            stepsGoal = prefs.stepsGoal,
            rhrAvgThisWeek = rhrThis,
            rhrAvgLastWeek = rhrLast,
            rhrDelta = rhrDelta,
            avgStepsThisWeek = avgStepsThis,
            avgStepsLastWeek = avgStepsLast,
        )
    }

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
                val ds = runCatching { repo.dailySummary(d) }.getOrNull()
                val sl = runCatching { repo.sleepOnDate(d) }.getOrNull()
                Triple(d, ds, sl)
            }
            val weeklySteps = weekly.map { (d, ds, _) -> d to (ds?.steps ?: 0L) }
            val weeklyCal = weekly.map { (d, ds, _) -> d to (ds?.totalCalories ?: 0.0) }
            val weeklySleep = weekly.map { (d, _, sl) -> d to (sl?.total?.toMinutes() ?: 0L) }

            val insights = runCatching { computeInsights(date) }.getOrNull()
            val bodyBattery = runCatching { computeBodyBattery(date, sleep) }.getOrNull()
            val anomalies = runCatching { computeAnomalies(date) }.getOrDefault(emptyList())

            _state.value = HomeState(
                loading = false,
                date = date,
                daily = daily,
                previousDaily = previous,
                sleep = sleep,
                readiness = readiness,
                bodyBattery = bodyBattery,
                weeklySteps = weeklySteps,
                weeklyCalories = weeklyCal,
                weeklySleepMin = weeklySleep,
                insights = insights,
                anomalies = anomalies,
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
