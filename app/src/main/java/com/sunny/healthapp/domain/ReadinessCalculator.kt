package com.sunny.healthapp.domain

import com.sunny.healthapp.domain.model.ReadinessContribution
import com.sunny.healthapp.domain.model.ReadinessSummary
import com.sunny.healthapp.domain.model.SleepSummary

/**
 * Heuristic readiness score (0-100) derived from inputs Health Connect typically exposes.
 * Not the Oura proprietary formula; treat as directional.
 */
object ReadinessCalculator {

    fun compute(
        lastNight: SleepSummary?,
        rhrBpm: Int?,
        rhrBaselineBpm: Int?,
        hrvMs: Double?,
        hrvBaselineMs: Double?,
        recentSleepHours: List<Double>,
    ): ReadinessSummary {
        val contributions = mutableListOf<ReadinessContribution>()

        val sleepScore = lastNight?.score ?: 60
        contributions += ReadinessContribution("Sleep", sleepScore)

        val rhrScore = if (rhrBpm != null && rhrBaselineBpm != null) {
            val delta = rhrBpm - rhrBaselineBpm
            (85 - delta * 3).coerceIn(40, 100)
        } else 70
        contributions += ReadinessContribution("Resting HR", rhrScore)

        val hrvScore = if (hrvMs != null && hrvBaselineMs != null && hrvBaselineMs > 0) {
            val ratio = hrvMs / hrvBaselineMs
            (60 + (ratio - 1) * 120).toInt().coerceIn(40, 100)
        } else 70
        contributions += ReadinessContribution("HRV balance", hrvScore)

        val avg = recentSleepHours.takeIf { it.isNotEmpty() }?.average() ?: 7.0
        val balanceScore = (avg / 8.0 * 100).toInt().coerceIn(40, 100)
        contributions += ReadinessContribution("Sleep balance", balanceScore)

        val overall = contributions.map { it.score }.average().toInt().coerceIn(0, 100)
        return ReadinessSummary(score = overall, contributions = contributions)
    }
}
