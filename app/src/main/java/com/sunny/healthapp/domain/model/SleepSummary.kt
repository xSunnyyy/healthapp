package com.sunny.healthapp.domain.model

import java.time.Duration
import java.time.Instant

enum class SleepStage { Awake, Light, Deep, REM, Unknown }

data class SleepSegment(
    val stage: SleepStage,
    val start: Instant,
    val end: Instant,
) {
    val duration: Duration get() = Duration.between(start, end)
}

data class SleepSummary(
    val start: Instant,
    val end: Instant,
    val timeInBed: Duration,
    val awake: Duration,
    val light: Duration,
    val deep: Duration,
    val rem: Duration,
    val segments: List<SleepSegment>,
    val avgHeartRate: Int?,
    val avgHrv: Double?,
    val avgSpo2: Double?,
    val avgRespiratoryRate: Double?,
) {
    // Matches what Google Health / Fitbit show: sum of asleep stages.
    val total: Duration get() = light + deep + rem

    val efficiencyPct: Int
        get() = if (timeInBed.isZero) 0
        else ((total.toMinutes().toDouble() / timeInBed.toMinutes()) * 100).toInt()

    val score: Int get() {
        // Composite 0-100 score derived from duration, efficiency, deep+REM share.
        val hours = total.toMinutes() / 60.0
        val durationScore = ((hours / 8.0).coerceAtMost(1.0)) * 40
        val efficiencyScore = (efficiencyPct.coerceAtMost(100) / 100.0) * 30
        val totalMin = total.toMinutes().coerceAtLeast(1)
        val restorative = (deep.toMinutes() + rem.toMinutes()).toDouble() / totalMin
        val restorativeScore = (restorative.coerceAtMost(0.5) / 0.5) * 30
        return (durationScore + efficiencyScore + restorativeScore).toInt().coerceIn(0, 100)
    }
}
