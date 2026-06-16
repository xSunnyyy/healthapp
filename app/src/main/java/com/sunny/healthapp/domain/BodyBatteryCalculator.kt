package com.sunny.healthapp.domain

import com.sunny.healthapp.domain.model.BodyBatteryPoint
import com.sunny.healthapp.domain.model.BodyBatteryStatus
import com.sunny.healthapp.domain.model.BodyBatterySummary
import com.sunny.healthapp.domain.model.SleepSummary
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Computes a Body-Battery-style 0-100 energy reserve.
 *
 * - Morning charge is built from sleep, HRV/RHR vs your own 14-day baseline,
 *   and bedtime consistency. None of these need weight or height — they're
 *   self-referential ratios.
 * - From wake forward we drain on a per-30-minute bucket basis: passive time
 *   awake + activity (active kcal) + minutes in elevated/high HR zones.
 * - We recharge during sustained calm periods (HR ≤ resting + no steps) and
 *   for any detected nap inside the day.
 *
 * Storage stays SI (kcal, bpm, minutes). All math here is unitless ratios
 * folded into a final 0..100 number.
 */
object BodyBatteryCalculator {

    data class Inputs(
        val now: Instant,
        val zone: ZoneId,
        val lastNightSleep: SleepSummary?,
        // Baselines computed from the last 14 nights of data.
        val hrvBaselineMs: Double?,
        val rhrBaselineBpm: Int?,
        val avgBedtime: LocalTime?,
        // Time-series for today (since wake) sorted by time.
        val hrSamples: List<Pair<Instant, Int>>,
        val activeKcalByHour: Map<Int, Double>, // hour-of-day (0..23) -> kcal
        val stepsByMinute: Map<Long, Long>,     // epochMinute -> step count
        // Daytime naps detected within today (start, end).
        val napsToday: List<Pair<Instant, Instant>>,
    )

    fun compute(inputs: Inputs): BodyBatterySummary {
        val (sleepQuality, hrvBalance, rhrBalance, sleepConsistency) = morningContributors(inputs)
        val charge = (
            100.0 * (
                0.40 * sleepQuality +
                0.30 * hrvBalance +
                0.20 * rhrBalance +
                0.10 * sleepConsistency
            )
        ).coerceIn(0.0, 100.0)

        val wakeInstant = inputs.lastNightSleep?.end
            ?: inputs.now.atZone(inputs.zone).toLocalDate().atStartOfDay(inputs.zone)
                .plusHours(7).toInstant() // assume 7am wake if no sleep
        val curve = integrateDay(inputs, charge, wakeInstant)

        val current = (curve.lastOrNull()?.value ?: charge.toInt()).coerceIn(0, 100)
        val status = when {
            inputs.lastNightSleep == null && inputs.hrSamples.isEmpty() -> BodyBatteryStatus.NoData
            current >= 70 -> BodyBatteryStatus.Charged
            current >= 40 -> BodyBatteryStatus.Coasting
            else -> BodyBatteryStatus.Depleted
        }
        return BodyBatterySummary(
            current = current,
            morningCharge = charge.toInt(),
            status = status,
            curve = curve,
            sleepQuality = sleepQuality,
            hrvBalance = hrvBalance,
            rhrBalance = rhrBalance,
            sleepConsistency = sleepConsistency,
        )
    }

    private data class Contributors(
        val sleepQuality: Double,
        val hrvBalance: Double,
        val rhrBalance: Double,
        val sleepConsistency: Double,
    )

    private fun morningContributors(inputs: Inputs): Contributors {
        val sleep = inputs.lastNightSleep
        // Sleep quality: duration toward 8h × efficiency × deep+REM share.
        val sleepQuality = if (sleep == null || sleep.total.toMinutes() <= 0) {
            0.5
        } else {
            val hours = sleep.total.toMinutes() / 60.0
            val durationScore = (hours / 8.0).coerceIn(0.0, 1.0)
            val efficiency = (sleep.efficiencyPct / 100.0).coerceIn(0.0, 1.0)
            val restorative =
                (sleep.deep.toMinutes() + sleep.rem.toMinutes()).toDouble() /
                    sleep.total.toMinutes().toDouble()
            val restorativeMultiplier = 0.5 + restorative.coerceIn(0.0, 0.5)
            (durationScore * efficiency * restorativeMultiplier).coerceIn(0.0, 1.0)
        }

        // HRV balance: today vs your 14-day baseline. 1.0 = exactly average; >1 boosts.
        val hrvBalance = if (
            inputs.lastNightSleep?.avgHrv != null &&
            inputs.hrvBaselineMs != null &&
            inputs.hrvBaselineMs > 0
        ) {
            val ratio = inputs.lastNightSleep.avgHrv!! / inputs.hrvBaselineMs
            ((ratio - 0.7) / 0.6).coerceIn(0.0, 1.0)
        } else 0.7

        // RHR balance: each 1 bpm above baseline costs 10% of this contributor.
        val rhrBalance = if (
            inputs.lastNightSleep?.avgHeartRate != null &&
            inputs.rhrBaselineBpm != null
        ) {
            val delta = inputs.lastNightSleep.avgHeartRate!! - inputs.rhrBaselineBpm
            (1.0 - delta.coerceAtLeast(0) / 10.0).coerceIn(0.0, 1.0)
        } else 0.7

        // Bedtime consistency: deviation from your 7-day avg bedtime, 2h = 0.
        val sleepConsistency = if (sleep != null && inputs.avgBedtime != null) {
            val bedtime = sleep.start.atZone(inputs.zone).toLocalTime()
            // Wrap to nearest distance through midnight (e.g. 23:50 vs 00:10 = 20 min, not 23h 40m).
            val rawDiff = abs(
                ChronoUnit.MINUTES.between(inputs.avgBedtime, bedtime).toDouble()
            )
            val minutes = minOf(rawDiff, 24 * 60 - rawDiff)
            (1.0 - minutes / 120.0).coerceIn(0.0, 1.0)
        } else 0.7

        return Contributors(sleepQuality, hrvBalance, rhrBalance, sleepConsistency)
    }

    private fun integrateDay(
        inputs: Inputs,
        startCharge: Double,
        wake: Instant,
    ): List<BodyBatteryPoint> {
        val now = inputs.now
        if (now <= wake) return listOf(point(inputs.zone, wake, startCharge))

        val bucketMinutes = 30L
        var battery = startCharge

        val curve = mutableListOf(point(inputs.zone, wake, battery))
        var bucketStart = wake
        while (bucketStart < now) {
            val bucketEnd = minOf(now, bucketStart.plus(Duration.ofMinutes(bucketMinutes)))
            val bucketDurationMin = Duration.between(bucketStart, bucketEnd).toMinutes()
            if (bucketDurationMin <= 0) break

            // Group raw HR samples by clock-minute so we count MINUTES in each
            // zone, not raw samples. Fitbit writes ~1 sample every 1-5s; without
            // this grouping a single hour of 'elevated' HR would be counted as
            // thousands of zone-minutes and drain the battery to 0 instantly.
            val hrInBucket = inputs.hrSamples.filter {
                it.first >= bucketStart && it.first < bucketEnd
            }
            val byMinute = hrInBucket.groupBy { it.first.epochSecond / 60 }

            val elevatedMin = byMinute.count { (_, samples) ->
                samples.any { it.second in 110..149 }
            }
            val highMin = byMinute.count { (_, samples) ->
                samples.any { it.second >= 150 }
            }

            // ----- Drain -----
            val passive = 2.5 * (bucketDurationMin / 60.0)
            val zoneDrain = 0.20 * elevatedMin + 0.60 * highMin
            val hourLocal = bucketStart.atZone(inputs.zone).hour
            val bucketActiveKcal =
                (inputs.activeKcalByHour[hourLocal] ?: 0.0) * (bucketDurationMin / 60.0)
            val activityDrain = bucketActiveKcal * 0.03

            // ----- Recharge -----
            // A 'quiet minute' is one where every HR sample is <= 75 bpm AND
            // no steps were recorded that minute. Grouped by clock-minute so
            // we credit minutes, not the raw sample count.
            val quietMinutes = byMinute.count { (epochMin, samples) ->
                samples.all { it.second <= 75 } &&
                    (inputs.stepsByMinute[epochMin] ?: 0L) == 0L
            }
            val quietRecharge = quietMinutes * 0.10

            val napRecharge = inputs.napsToday.sumOf { (napStart, napEnd) ->
                val overlapStart = maxOf(napStart, bucketStart)
                val overlapEnd = minOf(napEnd, bucketEnd)
                if (overlapEnd > overlapStart) {
                    Duration.between(overlapStart, overlapEnd).toMinutes() * 0.4
                } else 0.0
            }

            battery = (battery - passive - zoneDrain - activityDrain + quietRecharge + napRecharge)
                .coerceIn(0.0, 100.0)
            curve += point(inputs.zone, bucketEnd, battery)
            bucketStart = bucketEnd
        }
        return curve
    }

    private fun point(zone: ZoneId, at: Instant, value: Double): BodyBatteryPoint {
        val local = at.atZone(zone).toLocalTime()
        val min = local.hour * 60 + local.minute
        return BodyBatteryPoint(minuteOfDay = min, value = value.toInt().coerceIn(0, 100))
    }
}
