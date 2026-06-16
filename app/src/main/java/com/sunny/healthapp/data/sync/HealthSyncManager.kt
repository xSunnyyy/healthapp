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
import androidx.health.connect.client.records.metadata.DataOrigin
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
import com.sunny.healthapp.data.prefs.UserPrefsRepository
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
    data class Done(val at: Instant, val report: SyncReport, val source: String? = null) : SyncStatus()
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
    private val prefs: UserPrefsRepository? = null,
) {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    @Volatile private var primaryOrigin: DataOrigin? = null
    @Volatile private var availableOrigins: List<OriginInfo> = emptyList()

    /** The package name of the data source we're filtering by, e.g. "com.fitbit.FitbitMobile". */
    fun primarySource(): String? = primaryOrigin?.packageName

    /** Every distinct source seen in the last 30 days of steps, with record counts. */
    fun availableSources(): List<String> =
        availableOrigins.map {
            com.sunny.healthapp.util.Sources.friendlyLine(it.packageName, it.recordCount)
        }

    fun availableOriginPackages(): List<String> = availableOrigins.map { it.packageName }

    data class OriginInfo(val packageName: String, val recordCount: Int)

    class NoFitbitSourceException(message: String) : RuntimeException(message)

    companion object {
        private const val TAG = "HealthSync"
        const val BACKFILL_DAYS = 365L
        const val INCREMENTAL_OVERLAP_DAYS = 2L
        private const val META_KEY = "global"

        // Only origins whose package name contains this string are accepted.
        // The user explicitly wants Fitbit data only — no phone pedometer,
        // no Google Fit aggregation, no anything else.
        private const val ALLOWED_PACKAGE_PATTERN = "fitbit"
    }

    /**
     * Discover the Fitbit data source. If no origin containing 'fitbit' is found,
     * returns null and the caller surfaces the error to the user. We intentionally
     * do NOT fall back to phone pedometers, Google Fit or other origins.
     */
    private suspend fun discoverPrimaryOrigin(): DataOrigin? {
        val window = TimeRangeFilter.between(
            Instant.now().minus(Duration.ofDays(30)),
            Instant.now(),
        )
        val records = hc.read(StepsRecord::class, window)
        if (records.isEmpty()) {
            availableOrigins = emptyList()
            return null
        }

        val counts = records.groupingBy { it.metadata.dataOrigin }.eachCount()
        availableOrigins = counts
            .map { (origin, count) -> OriginInfo(origin.packageName, count) }
            .sortedByDescending { it.recordCount }
        Log.i(TAG, "Data origins seen for steps: ${availableSources()}")

        val origins = records.map { it.metadata.dataOrigin }.distinct()

        // 1) Honor an explicit user override if it points at a real origin.
        val override = prefs?.current()?.preferredOrigin
        if (override != null) {
            val match = origins.firstOrNull { it.packageName == override }
            if (match != null) {
                Log.i(TAG, "Using user override: ${match.packageName}")
                return match
            }
            Log.w(TAG, "User override $override not present in HC any more; falling back to Fitbit auto-detect")
        }

        // 2) STRICT: only a Fitbit-named origin. No fallback.
        val fitbit = origins.firstOrNull {
            it.packageName.contains(ALLOWED_PACKAGE_PATTERN, ignoreCase = true)
        }
        if (fitbit != null) {
            Log.i(TAG, "Using Fitbit origin: ${fitbit.packageName}")
            return fitbit
        }

        Log.w(TAG, "No Fitbit data source found. Origins seen: ${availableSources()}")
        return null
    }

    suspend fun syncAll(force: Boolean = false): SyncReport {
        _status.value = SyncStatus.Syncing(progress = 0f, message = "Reading Health Connect…")
        try {
            val report = doSync(force)
            _status.value = SyncStatus.Done(
                at = Instant.now(),
                report = report,
                source = primaryOrigin?.packageName,
            )
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

        // Pick the data source once at the start of the sync. Cached on the
        // instance so per-day calls all use the same filter.
        primaryOrigin = discoverPrimaryOrigin()
        if (primaryOrigin == null) {
            throw NoFitbitSourceException(
                "No Fitbit data found in Health Connect.\n\n" +
                    "Open the Fitbit app → Profile → app settings → Health Connect, " +
                    "and turn ON Steps, Heart rate, Sleep, Distance and Active energy. " +
                    "Then come back and pull down to refresh."
            )
        }
        val originFilter: Set<DataOrigin> = setOf(primaryOrigin!!)

        // On a force-resync, wipe everything we previously stored so any rows
        // that were upserted from non-Fitbit sources before strict mode are
        // gone. After this the only data in Room comes from the Fitbit origin.
        if (force) {
            Log.i(TAG, "Force sync: clearing local cache before backfill")
            db.dailySummaryDao().clearAll()
            db.sleepDao().clearAll()
            db.hrSampleDao().clearAll()
            db.hrvSampleDao().clearAll()
            db.spo2SampleDao().clearAll()
        }

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
                // Aggregate ONLY from the primary data source we discovered so
                // totals match Google Health's pick-one-source behavior instead
                // of summing Fitbit + phone pedometer + Google Fit etc.
                val agg = hc.aggregate(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL,
                        FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL,
                        HeartRateRecord.BPM_AVG,
                        HeartRateRecord.BPM_MIN,
                        HeartRateRecord.BPM_MAX,
                        RestingHeartRateRecord.BPM_AVG,
                    ),
                    range = range,
                    dataOriginFilter = originFilter,
                )

                val steps = agg?.get(StepsRecord.COUNT_TOTAL) ?: 0L
                val active = agg?.get(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)?.inKilocalories ?: 0.0
                val total = agg?.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)?.inKilocalories ?: 0.0
                val distance = agg?.get(DistanceRecord.DISTANCE_TOTAL)?.inMeters ?: 0.0
                val floors = agg?.get(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL) ?: 0.0
                val aggAvgHr = agg?.get(HeartRateRecord.BPM_AVG)?.toInt()
                val aggMinHr = agg?.get(HeartRateRecord.BPM_MIN)?.toInt()
                val aggMaxHr = agg?.get(HeartRateRecord.BPM_MAX)?.toInt()
                val aggRhr = agg?.get(RestingHeartRateRecord.BPM_AVG)?.toInt()

                val exerciseMin = hc.read(ExerciseSessionRecord::class, range, originFilter)
                    .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }

                // Pull raw HR samples for charting (aggregate doesn't include time-series)
                val hrRecords = hc.read(HeartRateRecord::class, range, originFilter)
                val samplesInDay = hrRecords.flatMap { r ->
                    r.samples.map { it.time to it.beatsPerMinute.toInt() }
                }.sortedBy { it.first }

                val entity = DailySummaryEntity(
                    date = d,
                    steps = steps,
                    activeCalories = active,
                    totalCalories = if (total > 0) total else active,
                    distanceMeters = distance,
                    exerciseMinutes = exerciseMin,
                    floorsClimbed = floors,
                    avgHeartRate = aggAvgHr,
                    minHeartRate = aggMinHr,
                    maxHeartRate = aggMaxHr,
                    latestHeartRate = samplesInDay.lastOrNull()?.second,
                    restingHeartRate = aggRhr,
                )
                db.dailySummaryDao().upsert(entity)
                daysWritten++

                if (samplesInDay.isNotEmpty()) {
                    db.hrSampleDao().insertAll(samplesInDay.map { HrSampleEntity(it.first, it.second) })
                    hrWritten += samplesInDay.size
                }

                val hrvSamples = hc.read(HeartRateVariabilityRmssdRecord::class, range, originFilter)
                if (hrvSamples.isNotEmpty()) {
                    db.hrvSampleDao().insertAll(
                        hrvSamples.map { HrvSampleEntity(it.time, it.heartRateVariabilityMillis) }
                    )
                    hrvWritten += hrvSamples.size
                }

                val spo2Samples = hc.read(OxygenSaturationRecord::class, range, originFilter)
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
            val sessions = hc.read(SleepSessionRecord::class, fullRange, originFilter)
            sessions.forEach { s ->
                // Sum precise Durations, then convert to minutes once at the end —
                // prevents the per-segment truncation that caused 1h54m vs 1h56m mismatches.
                val byStage = s.stages.groupBy { stageName(it.stage) }
                    .mapValues { (_, list) ->
                        list.fold(Duration.ZERO) { a, st ->
                            a + Duration.between(st.startTime, st.endTime)
                        }.toMinutes()
                    }
                val deep = byStage["DEEP"] ?: 0
                val light = byStage["LIGHT"] ?: 0
                val rem = byStage["REM"] ?: 0
                val awake = byStage["AWAKE"] ?: 0
                val tib = Duration.between(s.startTime, s.endTime).toMinutes()

                val sessionRange = TimeRangeFilter.between(s.startTime, s.endTime)
                val sleepHr = hc.read(HeartRateRecord::class, sessionRange, originFilter)
                    .flatMap { it.samples }.map { it.beatsPerMinute.toInt() }
                    .takeIf { it.isNotEmpty() }?.average()?.toInt()
                val sleepHrv = hc.read(HeartRateVariabilityRmssdRecord::class, sessionRange, originFilter)
                    .map { it.heartRateVariabilityMillis }
                    .takeIf { it.isNotEmpty() }?.average()
                val sleepSpo2 = hc.read(OxygenSaturationRecord::class, sessionRange, originFilter)
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
