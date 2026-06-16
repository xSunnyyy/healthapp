package com.sunny.healthapp.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregateMetric
import java.time.Duration
import kotlin.reflect.KClass

enum class HealthConnectAvailability { Installed, ProviderUpdateRequired, NotSupported }

class HealthConnectManager(private val context: Context) {

    val availability: HealthConnectAvailability
        get() = when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Installed
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.ProviderUpdateRequired
            else -> HealthConnectAvailability.NotSupported
        }

    private val client: HealthConnectClient?
        get() = runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()

    val permissionContract get() = PermissionController.createRequestPermissionResultContract()

    suspend fun grantedPermissions(): Set<String> =
        client?.permissionController?.getGrantedPermissions().orEmpty()

    suspend fun hasAllPermissions(): Boolean =
        grantedPermissions().containsAll(HealthPermissions.READ)

    suspend fun <T : Record> read(
        type: KClass<T>,
        range: TimeRangeFilter,
        dataOriginFilter: Set<DataOrigin> = emptySet(),
        paginate: Boolean = true,
    ): List<T> {
        val c = client ?: return emptyList()
        return runCatching {
            if (!paginate) {
                // Single-page read — used when callers only need a sample
                // (e.g. origin discovery enumerating distinct sources) and not
                // every record in the range.
                return@runCatching c.readRecords(
                    ReadRecordsRequest(
                        recordType = type,
                        timeRangeFilter = range,
                        dataOriginFilter = dataOriginFilter,
                    )
                ).records
            }
            // Health Connect's ReadRecordsRequest caps at 1000 records per page
            // and returns them sorted ascending by start time. Iterate the
            // pageToken until HC tells us there's nothing more, with a 200-page
            // safety cap in case HC misbehaves.
            val all = mutableListOf<T>()
            var token: String? = null
            var pages = 0
            do {
                val response = c.readRecords(
                    ReadRecordsRequest(
                        recordType = type,
                        timeRangeFilter = range,
                        dataOriginFilter = dataOriginFilter,
                        pageToken = token,
                    )
                )
                all.addAll(response.records)
                token = response.pageToken
                pages++
                if (pages > 200) {
                    android.util.Log.w("HealthConnect", "read() page cap hit for $type")
                    break
                }
            } while (token != null)
            all
        }.getOrDefault(emptyList())
    }

    suspend fun aggregate(
        metrics: Set<AggregateMetric<*>>,
        range: TimeRangeFilter,
        dataOriginFilter: Set<DataOrigin> = emptySet(),
    ): AggregationResult? {
        val c = client ?: return null
        return runCatching {
            c.aggregate(
                AggregateRequest(
                    metrics = metrics,
                    timeRangeFilter = range,
                    dataOriginFilter = dataOriginFilter,
                )
            )
        }.getOrNull()
    }

    /**
     * Bucketed aggregate — returns per-slice results so callers can merge
     * across data sources. Used by phone-fill mode for steps/distance.
     */
    suspend fun aggregateByDuration(
        metrics: Set<AggregateMetric<*>>,
        range: TimeRangeFilter,
        slicer: Duration,
        dataOriginFilter: Set<DataOrigin> = emptySet(),
    ): List<androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration> {
        val c = client ?: return emptyList()
        return runCatching {
            c.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = metrics,
                    timeRangeFilter = range,
                    timeRangeSlicer = slicer,
                    dataOriginFilter = dataOriginFilter,
                )
            )
        }.getOrDefault(emptyList())
    }
}
