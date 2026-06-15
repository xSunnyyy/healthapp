package com.sunny.healthapp.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "sleep_session")
data class SleepSessionEntity(
    @PrimaryKey val id: String,
    val start: Instant,
    val end: Instant,
    val totalMin: Long,
    val timeInBedMin: Long,
    val deepMin: Long,
    val lightMin: Long,
    val remMin: Long,
    val awakeMin: Long,
    val avgHeartRate: Int?,
    val avgHrvMs: Double?,
    val avgSpo2Pct: Double?,
    val avgRespirationRpm: Double?,
)

@Entity(
    tableName = "sleep_stage",
    foreignKeys = [
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sessionId")],
)
data class SleepStageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val stage: String,
    val start: Instant,
    val end: Instant,
)
