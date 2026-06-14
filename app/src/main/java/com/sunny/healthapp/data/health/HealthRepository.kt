package com.sunny.healthapp.data.health

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.time.TimeRangeFilter
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

class HealthRepository(private val hc: HealthConnectManager) {

    suspend fun dailySummary(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): DailySummary {
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        val range = TimeRangeFilter.between(start, end)

        val steps = hc.read(StepsRecord::class, range).sumOf { it.count }
        val active = hc.read(ActiveCaloriesBurnedRecord::class, range)
            .sumOf { it.energy.inKilocalories }
        val total = hc.read(TotalCaloriesBurnedRecord::class, range)
            .sumOf { it.energy.inKilocalories }
        val distance = hc.read(DistanceRecord::class, range)
            .sumOf { it.distance.inMeters }
        val exercise = hc.read(ExerciseSessionRecord::class, range)
            .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
        val floors = hc.read(FloorsClimbedRecord::class, range).sumOf { it.floors }
        val hr = hc.read(HeartRateRecord::class, range)
            .flatMap { it.samples }.map { it.beatsPerMinute.toInt() }
        val rhr = hc.read(RestingHeartRateRecord::class, range)
            .map { it.beatsPerMinute.toInt() }.minOrNull()

        return DailySummary(
            date = date,
            steps = steps,
            activeCalories = active,
            totalCalories = if (total > 0) total else active,
            distanceMeters = distance,
            exerciseMinutes = exercise,
            floorsClimbed = floors,
            avgHeartRate = hr.takeIf { it.isNotEmpty() }?.average()?.toInt(),
            minHeartRate = hr.minOrNull(),
            maxHeartRate = hr.maxOrNull(),
            restingHeartRate = rhr,
        )
    }

    suspend fun lastNightSleep(zone: ZoneId = ZoneId.systemDefault()): SleepSummary? {
        val now = Instant.now()
        val from = now.minus(Duration.ofHours(36))
        val sessions = hc.read(SleepSessionRecord::class, TimeRangeFilter.between(from, now))
        val latest = sessions.maxByOrNull { it.endTime } ?: return null
        return buildSleepSummary(latest)
    }

    private suspend fun buildSleepSummary(s: SleepSessionRecord): SleepSummary {
        val range = TimeRangeFilter.between(s.startTime, s.endTime)
        val segments = s.stages.map { st ->
            SleepSegment(
                stage = when (st.stage) {
                    SleepSessionRecord.STAGE_TYPE_AWAKE,
                    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED,
                    SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> SleepStage.Awake
                    SleepSessionRecord.STAGE_TYPE_LIGHT,
                    SleepSessionRecord.STAGE_TYPE_SLEEPING -> SleepStage.Light
                    SleepSessionRecord.STAGE_TYPE_DEEP -> SleepStage.Deep
                    SleepSessionRecord.STAGE_TYPE_REM -> SleepStage.REM
                    else -> SleepStage.Unknown
                },
                start = st.startTime,
                end = st.endTime,
            )
        }
        val byStage = segments.groupBy { it.stage }
            .mapValues { (_, list) -> list.fold(Duration.ZERO) { acc, seg -> acc + seg.duration } }
        val awake = byStage[SleepStage.Awake] ?: Duration.ZERO
        val light = byStage[SleepStage.Light] ?: Duration.ZERO
        val deep = byStage[SleepStage.Deep] ?: Duration.ZERO
        val rem = byStage[SleepStage.REM] ?: Duration.ZERO
        val timeInBed = Duration.between(s.startTime, s.endTime)
        val asleep = timeInBed - awake

        val hr = hc.read(HeartRateRecord::class, range)
            .flatMap { it.samples }.map { it.beatsPerMinute.toInt() }
            .takeIf { it.isNotEmpty() }?.average()?.toInt()
        val hrv = hc.read(HeartRateVariabilityRmssdRecord::class, range)
            .map { it.heartRateVariabilityMillis }
            .takeIf { it.isNotEmpty() }?.average()
        val spo2 = hc.read(OxygenSaturationRecord::class, range)
            .map { it.percentage.value }
            .takeIf { it.isNotEmpty() }?.average()
        val rr = hc.read(RespiratoryRateRecord::class, range)
            .map { it.rate }
            .takeIf { it.isNotEmpty() }?.average()

        return SleepSummary(
            start = s.startTime,
            end = s.endTime,
            total = asleep,
            timeInBed = timeInBed,
            awake = awake,
            light = light,
            deep = deep,
            rem = rem,
            segments = segments,
            avgHeartRate = hr,
            avgHrv = hrv,
            avgSpo2 = spo2,
            avgRespiratoryRate = rr,
        )
    }

    suspend fun readiness(zone: ZoneId = ZoneId.systemDefault()): ReadinessSummary {
        val now = Instant.now()
        val sevenDays = TimeRangeFilter.between(now.minus(Duration.ofDays(7)), now)

        val rhrSamples = hc.read(RestingHeartRateRecord::class, sevenDays)
            .map { it.beatsPerMinute.toInt() }
        val latestRhr = rhrSamples.lastOrNull()
        val baselineRhr = rhrSamples.dropLast(1).takeIf { it.isNotEmpty() }?.average()?.toInt()

        val hrvSamples = hc.read(HeartRateVariabilityRmssdRecord::class, sevenDays)
            .map { it.heartRateVariabilityMillis }
        val latestHrv = hrvSamples.lastOrNull()
        val baselineHrv = hrvSamples.dropLast(1).takeIf { it.isNotEmpty() }?.average()

        val recentSleep = mutableListOf<Double>()
        val sessions = hc.read(SleepSessionRecord::class, sevenDays)
        sessions.forEach {
            recentSleep += Duration.between(it.startTime, it.endTime).toMinutes() / 60.0
        }

        return ReadinessCalculator.compute(
            lastNight = lastNightSleep(zone),
            rhrBpm = latestRhr,
            rhrBaselineBpm = baselineRhr,
            hrvMs = latestHrv,
            hrvBaselineMs = baselineHrv,
            recentSleepHours = recentSleep,
        )
    }
}
