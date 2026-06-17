package com.sunny.healthapp.domain

import com.sunny.healthapp.domain.model.AnomalyInsight
import com.sunny.healthapp.domain.model.AnomalyKind
import com.sunny.healthapp.domain.model.AnomalySeverity
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Rolling Z-score outlier detection across resting HR, HRV, sleep duration
 * and step count. We need at least 14 days of history per metric for the
 * baseline to be meaningful — below that, the detector skips that metric.
 *
 * Thresholds (in standard deviations from the rolling mean):
 *   - |z| ≥ 1.5  → notable
 *   - |z| ≥ 2.2  → strong signal (severity = High)
 */
object AnomalyDetector {

    private const val MIN_HISTORY = 14
    private const val NOTABLE_Z = 1.5
    private const val STRONG_Z = 2.2

    data class Inputs(
        val rhrToday: Int?,
        val rhrHistory: List<Int>,
        val hrvToday: Double?,
        val hrvHistory: List<Double>,
        val sleepMinToday: Long?,
        val sleepMinHistory: List<Long>,
        val stepsToday: Long?,
        val stepsHistory: List<Long>,
    )

    fun detect(inputs: Inputs): List<AnomalyInsight> {
        val out = mutableListOf<AnomalyInsight>()

        rhr(inputs)?.let(out::add)
        hrv(inputs)?.let(out::add)
        sleep(inputs)?.let(out::add)
        steps(inputs)?.let(out::add)

        return out.sortedByDescending { it.severity }
    }

    private fun rhr(inputs: Inputs): AnomalyInsight? {
        val today = inputs.rhrToday ?: return null
        val hist = inputs.rhrHistory
        if (hist.size < MIN_HISTORY) return null
        val mean = hist.average()
        val sd = stdDev(hist.map { it.toDouble() }, mean)
        if (sd <= 0) return null
        val z = (today - mean) / sd
        if (z < NOTABLE_Z) return null
        val delta = today - mean.toInt()
        return AnomalyInsight(
            kind = AnomalyKind.RestingHrUp,
            title = "Resting HR up +$delta bpm",
            description = "${today} bpm vs ${mean.toInt()} bpm 30-day average. " +
                "Often a sign of illness onset, hard prior workout, or stress.",
            severity = if (z >= STRONG_Z) AnomalySeverity.High else AnomalySeverity.Medium,
        )
    }

    private fun hrv(inputs: Inputs): AnomalyInsight? {
        val today = inputs.hrvToday ?: return null
        val hist = inputs.hrvHistory
        if (hist.size < MIN_HISTORY) return null
        val mean = hist.average()
        val sd = stdDev(hist, mean)
        if (sd <= 0) return null
        val z = (today - mean) / sd
        if (z > -NOTABLE_Z) return null
        val pct = ((today / mean - 1.0) * 100).toInt()
        return AnomalyInsight(
            kind = AnomalyKind.HrvDown,
            title = "HRV down $pct%",
            description = "${today.toInt()} ms vs ${mean.toInt()} ms 30-day average. " +
                "HRV drops often precede an illness or follow a heavy day. Take it gentle.",
            severity = if (z <= -STRONG_Z) AnomalySeverity.High else AnomalySeverity.Medium,
        )
    }

    private fun sleep(inputs: Inputs): AnomalyInsight? {
        val today = inputs.sleepMinToday ?: return null
        val hist = inputs.sleepMinHistory
        if (hist.size < MIN_HISTORY) return null
        val mean = hist.average()
        val sd = stdDev(hist.map { it.toDouble() }, mean)
        if (sd <= 0) return null
        val z = (today - mean) / sd
        if (abs(z) < NOTABLE_Z) return null
        val isShort = z < 0
        val delta = abs(today - mean.toLong())
        return AnomalyInsight(
            kind = if (isShort) AnomalyKind.ShortSleep else AnomalyKind.LongSleep,
            title = if (isShort) "Short night" else "Long night",
            description = "${fmtDur(today)} vs ${fmtDur(mean.toLong())} average — " +
                "${if (isShort) "down" else "up"} ${fmtDur(delta)} from your usual.",
            severity = if (abs(z) >= STRONG_Z) AnomalySeverity.High else AnomalySeverity.Medium,
        )
    }

    private fun steps(inputs: Inputs): AnomalyInsight? {
        val today = inputs.stepsToday ?: return null
        val hist = inputs.stepsHistory
        if (hist.size < MIN_HISTORY) return null
        val mean = hist.average()
        val sd = stdDev(hist.map { it.toDouble() }, mean)
        if (sd <= 0) return null
        val z = (today - mean) / sd
        if (abs(z) < NOTABLE_Z) return null
        val isLow = z < 0
        return AnomalyInsight(
            kind = if (isLow) AnomalyKind.StepsLow else AnomalyKind.StepsHigh,
            title = if (isLow) "Quiet day" else "Active day",
            description = "%,d steps vs %,d 30-day average.".format(today, mean.toLong()),
            severity = AnomalySeverity.Low,
        )
    }

    private fun stdDev(values: List<Double>, mean: Double): Double {
        if (values.isEmpty()) return 0.0
        val sumSq = values.sumOf { (it - mean) * (it - mean) }
        return sqrt(sumSq / values.size)
    }

    private fun fmtDur(min: Long): String {
        val absMin = abs(min)
        val h = absMin / 60
        val m = absMin % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}
