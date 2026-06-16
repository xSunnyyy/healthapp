package com.sunny.healthapp.data.health

import com.sunny.healthapp.data.db.HealthDatabase
import com.sunny.healthapp.data.db.entities.DailySummaryEntity
import com.sunny.healthapp.data.db.entities.SleepSessionEntity
import com.sunny.healthapp.data.db.entities.SleepStageEntity
import com.sunny.healthapp.domain.ReadinessCalculator
import com.sunny.healthapp.domain.model.DailySummary
import com.sunny.healthapp.domain.model.ReadinessSummary
import com.sunny.healthapp.domain.model.SleepSegment
import com.sunny.healthapp.domain.model.SleepStage
import com.sunny.healthapp.domain.model.SleepSummary
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reads from the local Room cache (populated by HealthSyncManager).
 * Every call returns a domain model whether or not a sync has happened —
 * callers get empty/null on cache miss without any HC hit.
 */
class HealthRepository(
    private val hc: HealthConnectManager,
    private val db: HealthDatabase,
) {

    suspend fun dailySummary(date: LocalDate): DailySummary {
        val row = db.dailySummaryDao().get(date)
        return row?.toDomain() ?: DailySummary(
            date = date,
            steps = 0L,
            activeCalories = 0.0,
            totalCalories = 0.0,
            distanceMeters = 0.0,
            exerciseMinutes = 0L,
            floorsClimbed = 0.0,
            avgHeartRate = null,
            minHeartRate = null,
            maxHeartRate = null,
            latestHeartRate = null,
            restingHeartRate = null,
        )
    }

    suspend fun dailyRange(start: LocalDate, end: LocalDate): List<DailySummary> =
        db.dailySummaryDao().range(start, end).map { it.toDomain() }

    data class HrSample(val time: Instant, val bpm: Int)
    suspend fun hrSamples(from: Instant, to: Instant): List<HrSample> =
        db.hrSampleDao().range(from, to).map { HrSample(it.time, it.bpm) }

    /** Average HRV across the last [days] nights of sleep sessions. */
    suspend fun hrvBaseline(days: Int = 14): Double? {
        val now = Instant.now()
        val from = now.minus(Duration.ofDays(days.toLong()))
        val sessions = db.sleepDao().range(from, now)
        val values = sessions.mapNotNull { it.avgHrvMs }
        return if (values.isEmpty()) null else values.average()
    }

    /** Average resting HR across the last [days] daily summaries. */
    suspend fun rhrBaseline(days: Int = 14): Int? {
        val today = LocalDate.now()
        val from = today.minusDays(days.toLong())
        val values = db.dailySummaryDao().range(from, today).mapNotNull { it.restingHeartRate }
        return if (values.isEmpty()) null else values.average().toInt()
    }

    /** Median bedtime over the last [days] sleep sessions. */
    suspend fun bedtimeAverage(zone: ZoneId = ZoneId.systemDefault(), days: Int = 7): java.time.LocalTime? {
        val now = Instant.now()
        val from = now.minus(Duration.ofDays(days.toLong()))
        val sessions = db.sleepDao().range(from, now)
        if (sessions.isEmpty()) return null
        val avgMinutes = sessions
            .map { it.start.atZone(zone).toLocalTime() }
            .map { it.hour * 60 + it.minute }
            .average()
            .toInt()
        return java.time.LocalTime.of((avgMinutes / 60) % 24, avgMinutes % 60)
    }

    suspend fun lastNightSleep(zone: ZoneId = ZoneId.systemDefault()): SleepSummary? {
        val s = db.sleepDao().latest() ?: return null
        val stages = db.sleepDao().stagesFor(s.id)
        return s.toDomain(stages)
    }

    /** All sleep sessions whose end time falls in [from, to). Newest first. */
    suspend fun sleepSessionsRange(from: Instant, to: Instant): List<SleepSummary> =
        db.sleepDao().range(from, to)
            .sortedByDescending { it.end }
            .map { session ->
                val stages = db.sleepDao().stagesFor(session.id)
                session.toDomain(stages)
            }

    suspend fun sleepOnDate(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): SleepSummary? {
        // Find the sleep session whose "wake time" lands on this date.
        val windowStart = date.minusDays(1).atStartOfDay(zone).toInstant()
        val windowEnd = date.plusDays(1).atStartOfDay(zone).toInstant()
        val sessions = db.sleepDao().range(windowStart, windowEnd)
        val s = sessions.maxByOrNull { it.end } ?: return null
        val stages = db.sleepDao().stagesFor(s.id)
        return s.toDomain(stages)
    }

    suspend fun readiness(): ReadinessSummary {
        val today = LocalDate.now()
        val sevenDays = (0..6).map { today.minusDays(it.toLong()) }
        val summaries = sevenDays.mapNotNull { db.dailySummaryDao().get(it) }
        val recentSleepHours = (0..6).mapNotNull { offset ->
            sleepOnDate(today.minusDays(offset.toLong()))?.total?.toMinutes()?.toDouble()?.div(60.0)
        }

        val rhrSamples = summaries.mapNotNull { it.restingHeartRate }
        val latestRhr = rhrSamples.firstOrNull()
        val baselineRhr = rhrSamples.drop(1).takeIf { it.isNotEmpty() }?.average()?.toInt()

        // Pull recent HRV from the samples table
        val now = Instant.now()
        val weekAgo = now.minus(Duration.ofDays(7))
        val hrvSamples = db.hrvSampleDao().range(weekAgo, now).map { it.rmssdMs }
        val latestHrv = hrvSamples.lastOrNull()
        val baselineHrv = hrvSamples.dropLast(1).takeIf { it.isNotEmpty() }?.average()

        return ReadinessCalculator.compute(
            lastNight = lastNightSleep(),
            rhrBpm = latestRhr,
            rhrBaselineBpm = baselineRhr,
            hrvMs = latestHrv,
            hrvBaselineMs = baselineHrv,
            recentSleepHours = recentSleepHours,
        )
    }

    private fun DailySummaryEntity.toDomain() = DailySummary(
        date = date,
        steps = steps,
        activeCalories = activeCalories,
        totalCalories = totalCalories,
        distanceMeters = distanceMeters,
        exerciseMinutes = exerciseMinutes,
        floorsClimbed = floorsClimbed,
        avgHeartRate = avgHeartRate,
        minHeartRate = minHeartRate,
        maxHeartRate = maxHeartRate,
        latestHeartRate = latestHeartRate,
        restingHeartRate = restingHeartRate,
    )

    private fun SleepSessionEntity.toDomain(stages: List<SleepStageEntity>) = SleepSummary(
        start = start,
        end = end,
        timeInBed = Duration.ofMinutes(timeInBedMin),
        awake = Duration.ofMinutes(awakeMin),
        light = Duration.ofMinutes(lightMin),
        deep = Duration.ofMinutes(deepMin),
        rem = Duration.ofMinutes(remMin),
        segments = stages.map { it.toDomain() },
        avgHeartRate = avgHeartRate,
        avgHrv = avgHrvMs,
        avgSpo2 = avgSpo2Pct,
        avgRespiratoryRate = avgRespirationRpm,
    )

    private fun SleepStageEntity.toDomain() = SleepSegment(
        stage = when (stage) {
            "AWAKE" -> SleepStage.Awake
            "REM" -> SleepStage.REM
            "LIGHT" -> SleepStage.Light
            "DEEP" -> SleepStage.Deep
            else -> SleepStage.Unknown
        },
        start = start,
        end = end,
    )
}
