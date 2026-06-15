package com.sunny.healthapp.data.sync

import android.util.Log
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.time.TimeRangeFilter
import com.sunny.healthapp.data.db.HealthDatabase
import com.sunny.healthapp.data.db.entities.DailySummaryEntity
import com.sunny.healthapp.data.db.entities.HrSampleEntity
import com.sunny.healthapp.data.db.entities.HrvSampleEntity
import com.sunny.healthapp.data.db.entities.SleepSessionEntity
import com.sunny.healthapp.data.db.entities.SleepStageEntity
import com.sunny.healthapp.data.db.entities.Spo2SampleEntity
import com.sunny.healthapp.data.db.entities.SyncStateEntity
import com.sunny.healthapp.data.health.HealthConnectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data class Syncing(val progress: Float = 0f, val message: String = "Syncing") : SyncStatus()
    data class Done(val at: Instant, val report: SyncReport) : SyncStatus()
    data class Error(val message: String?) : SyncStatus()
}

/**
 * Mirrors Health Connect data into the local Room cache.
 * First call → 365-day backfill. Subsequent calls → only re-sync the last 2 days
 * (where late-arriving samples land) plus any new days since last sync.
 */
class HealthSyncManager(
    private val hc: HealthConnectManager,
    private val db: HealthDatabase,
) {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    companion object {
        private const val TAG = "HealthSync"
        const val BACKFILL_DAYS = 365L
        const val INCREMENTAL_OVERLAP_DAYS = 2L
        private const val META_KEY = "global"
    }

    suspend fun syncAll(force: Boolean = false): SyncReport {
        _status.value = SyncStatus.Syncing(progress = 0f, message = "Reading Health Connect…")
        try {
            val report = doSync(force)
            _status.value = SyncStatus.Done(Instant.now(), report)
            return report
        } catch (e: Exception) {
            _status.value = SyncStatus.Error(e.message)
            throw e
        }
    }

    private suspend fun doSync(force: Boolean): SyncReport {
        val state = db.syncStateDao().get(META_KEY)
        val now = Instant.now()
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()

        val from: LocalDate = if (force || state?.lastFullSyncAt == null) {
            today.minusDays(BACKFILL_DAYS)
        } else {
            val latest = db.dailySummaryDao().latestStored() ?: today.minusDays(BACKFILL_DAYS)
            latest.minusDays(INCREMENTAL_OVERLAP_DAYS)
        }

        Log.i(TAG, "Sync window $from..$today (force=$force)")

        var daysWritten = 0
        var sleepSessionsWritten = 0
        var hrWritten = 0
        var hrvWritten = 0
        var spo2Written = 0

        val totalDays = (java.time.temporal.ChronoUnit.DAYS.between(from, today) + 1).toInt().coerceAtLeast(1)
        var dayIndex = 0

        // Per-day summaries — iterate over the range.
        var d = from
        while (!d.isAfter(today)) {
            dayIndex++
            if (dayIndex % 5 == 0 || dayIndex == 1) {
                _status.value = SyncStatus.Syncing(
                    progress = dayIndex.toFloat() / totalDays.toFloat() * 0.8f,
                    message = "Syncing day $dayIndex of $totalDays",
                )
            }
            val dayStart = d.atStartOfDay(zone).toInstant()
            val dayEnd = d.plusDays(1).atStartOfDay(zone).toInstant()
            val range = TimeRangeFilter.between(dayStart, dayEnd)

            try {
                val steps = hc.read(StepsRecord::class, range).sumOf { it.count }
                val active = hc.read(ActiveCaloriesBurnedRecord::class, range).sumOf { it.energy.inKilocalories }
                val total = hc.read(TotalCaloriesBurnedRecord::class, range).sumOf { it.energy.inKilocalories }
                val distance = hc.read(DistanceRecord::class, range).sumOf { it.distance.inMeters }
                val exerciseMin = hc.read(ExerciseSessionRecord::class, range)
                    .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
                val floors = hc.read(FloorsClimbedRecord::class, range).sumOf { it.floors }

                val hrRecords = hc.read(HeartRateRecord::class, range)
                val samplesInDay = hrRecords.flatMap { r ->
                    r.samples.map { it.time to it.beatsPerMinute.toInt() }
                }.sortedBy { it.first }
                val bpms = samplesInDay.map { it.second }
                val rhr = hc.read(RestingHeartRateRecord::class, range)
                    .map { it.beatsPerMinute.toInt() }.minOrNull()

                val entity = DailySummaryEntity(
                    date = d,
                    steps = steps,
                    activeCalories = active,
                    totalCalories = if (total > 0) total else active,
                    distanceMeters = distance,
                    exerciseMinutes = exerciseMin,
                    floorsClimbed = floors,
                    avgHeartRate = bpms.takeIf { it.isNotEmpty() }?.average()?.toInt(),
                    minHeartRate = bpms.minOrNull(),
                    maxHeartRate = bpms.maxOrNull(),
                    latestHeartRate = samplesInDay.lastOrNull()?.second,
                    restingHeartRate = rhr,
                )
                db.dailySummaryDao().upsert(entity)
                daysWritten++

                if (samplesInDay.isNotEmpty()) {
                    db.hrSampleDao().insertAll(samplesInDay.map { HrSampleEntity(it.first, it.second) })
                    hrWritten += samplesInDay.size
                }

                val hrvSamples = hc.read(HeartRateVariabilityRmssdRecord::class, range)
                if (hrvSamples.isNotEmpty()) {
                    db.hrvSampleDao().insertAll(
                        hrvSamples.map { HrvSampleEntity(it.time, it.heartRateVariabilityMillis) }
                    )
                    hrvWritten += hrvSamples.size
                }

                val spo2Samples = hc.read(OxygenSaturationRecord::class, range)
                if (spo2Samples.isNotEmpty()) {
                    db.spo2SampleDao().insertAll(
                        spo2Samples.map { Spo2SampleEntity(it.time, it.percentage.value) }
                    )
                    spo2Written += spo2Samples.size
                }
            } catch (e: Exception) {
                Log.w(TAG, "Day sync failed for $d", e)
            }

            d = d.plusDays(1)
        }

        _status.value = SyncStatus.Syncing(progress = 0.85f, message = "Syncing sleep history…")

        // Sleep — query the whole window in one shot, store sessions + stages
        try {
            val fullRange = TimeRangeFilter.between(
                from.atStartOfDay(zone).toInstant(),
                today.plusDays(1).atStartOfDay(zone).toInstant(),
            )
            val sessions = hc.read(SleepSessionRecord::class, fullRange)
            sessions.forEach { s ->
                val byStage = s.stages.groupBy { stageName(it.stage) }
                    .mapValues { (_, list) -> list.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() } }
                val deep = byStage["DEEP"] ?: 0
                val light = byStage["LIGHT"] ?: 0
                val rem = byStage["REM"] ?: 0
                val awake = byStage["AWAKE"] ?: 0
                val tib = Duration.between(s.startTime, s.endTime).toMinutes()

                val sessionRange = TimeRangeFilter.between(s.startTime, s.endTime)
                val sleepHr = hc.read(HeartRateRecord::class, sessionRange)
                    .flatMap { it.samples }.map { it.beatsPerMinute.toInt() }
                    .takeIf { it.isNotEmpty() }?.average()?.toInt()
                val sleepHrv = hc.read(HeartRateVariabilityRmssdRecord::class, sessionRange)
                    .map { it.heartRateVariabilityMillis }
                    .takeIf { it.isNotEmpty() }?.average()
                val sleepSpo2 = hc.read(OxygenSaturationRecord::class, sessionRange)
                    .map { it.percentage.value }
                    .takeIf { it.isNotEmpty() }?.average()

                val sessionEntity = SleepSessionEntity(
                    id = s.metadata.id,
                    start = s.startTime,
                    end = s.endTime,
                    totalMin = deep + light + rem,
                    timeInBedMin = tib,
                    deepMin = deep,
                    lightMin = light,
                    remMin = rem,
                    awakeMin = awake,
                    avgHeartRate = sleepHr,
                    avgHrvMs = sleepHrv,
                    avgSpo2Pct = sleepSpo2,
                    avgRespirationRpm = null,
                )
                val stages = s.stages.map { st ->
                    SleepStageEntity(
                        sessionId = sessionEntity.id,
                        stage = stageName(st.stage),
                        start = st.startTime,
                        end = st.endTime,
                    )
                }
                db.sleepDao().saveFull(sessionEntity, stages)
                sleepSessionsWritten++
            }
        } catch (e: Exception) {
            Log.w(TAG, "Sleep sync failed", e)
        }

        db.syncStateDao().upsert(
            SyncStateEntity(
                recordType = META_KEY,
                changeToken = null,
                lastFullSyncAt = state?.lastFullSyncAt ?: now.toEpochMilli(),
                lastIncrementalSyncAt = now.toEpochMilli(),
            )
        )

        val report = SyncReport(
            from = from,
            to = today,
            daysWritten = daysWritten,
            sleepSessionsWritten = sleepSessionsWritten,
            hrSamplesWritten = hrWritten,
            hrvSamplesWritten = hrvWritten,
            spo2SamplesWritten = spo2Written,
        )
        Log.i(TAG, "Sync complete: $report")
        return report
    }

    private fun stageName(stage: Int): String = when (stage) {
        SleepSessionRecord.STAGE_TYPE_AWAKE,
        SleepSessionRecord.STAGE_TYPE_OUT_OF_BED,
        SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> "AWAKE"
        SleepSessionRecord.STAGE_TYPE_LIGHT,
        SleepSessionRecord.STAGE_TYPE_SLEEPING -> "LIGHT"
        SleepSessionRecord.STAGE_TYPE_DEEP -> "DEEP"
        SleepSessionRecord.STAGE_TYPE_REM -> "REM"
        else -> "UNKNOWN"
    }
}

data class SyncReport(
    val from: LocalDate,
    val to: LocalDate,
    val daysWritten: Int,
    val sleepSessionsWritten: Int,
    val hrSamplesWritten: Int,
    val hrvSamplesWritten: Int,
    val spo2SamplesWritten: Int,
)
