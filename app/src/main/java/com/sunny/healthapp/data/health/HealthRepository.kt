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

    suspend fun lastNightSleep(zone: ZoneId = ZoneId.systemDefault()): SleepSummary? {
        val s = db.sleepDao().latest() ?: return null
        val stages = db.sleepDao().stagesFor(s.id)
        return s.toDomain(stages)
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
