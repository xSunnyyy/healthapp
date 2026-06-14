package com.sunny.healthapp.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregateMetric
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
    ): List<T> {
        val c = client ?: return emptyList()
        return runCatching {
            c.readRecords(ReadRecordsRequest(recordType = type, timeRangeFilter = range)).records
        }.getOrDefault(emptyList())
    }

    suspend fun aggregate(
        metrics: Set<AggregateMetric<*>>,
        range: TimeRangeFilter,
    ): AggregationResult? {
        val c = client ?: return null
        return runCatching { c.aggregate(AggregateRequest(metrics, range)) }.getOrNull()
    }
}
