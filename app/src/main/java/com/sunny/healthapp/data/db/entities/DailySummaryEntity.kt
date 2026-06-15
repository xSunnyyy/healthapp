package com.sunny.healthapp.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_summary")
data class DailySummaryEntity(
    @PrimaryKey val date: LocalDate,
    val steps: Long,
    val activeCalories: Double,
    val totalCalories: Double,
    val distanceMeters: Double,
    val exerciseMinutes: Long,
    val floorsClimbed: Double,
    val avgHeartRate: Int?,
    val minHeartRate: Int?,
    val maxHeartRate: Int?,
    val latestHeartRate: Int?,
    val restingHeartRate: Int?,
    val updatedAt: Long = System.currentTimeMillis(),
)
