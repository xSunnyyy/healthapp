package com.sunny.healthapp.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import java.time.Instant

@Entity(
    tableName = "hr_sample",
    primaryKeys = ["time"],
    indices = [Index("time")],
)
data class HrSampleEntity(
    val time: Instant,
    val bpm: Int,
)

@Entity(
    tableName = "hrv_sample",
    primaryKeys = ["time"],
    indices = [Index("time")],
)
data class HrvSampleEntity(
    val time: Instant,
    val rmssdMs: Double,
)

@Entity(
    tableName = "spo2_sample",
    primaryKeys = ["time"],
    indices = [Index("time")],
)
data class Spo2SampleEntity(
    val time: Instant,
    val pct: Double,
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @androidx.room.PrimaryKey val recordType: String,
    val changeToken: String?,
    val lastFullSyncAt: Long?,
    val lastIncrementalSyncAt: Long?,
)
