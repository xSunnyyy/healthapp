package com.sunny.healthapp.domain.model

import java.time.LocalDate

data class DailySummary(
    val date: LocalDate,
    val steps: Long,
    val activeCalories: Double,
    val totalCalories: Double,
    val distanceMeters: Double,
    val exerciseMinutes: Long,
    val floorsClimbed: Double,
    val avgHeartRate: Int?,
    val minHeartRate: Int?,
    val maxHeartRate: Int?,
    val restingHeartRate: Int?,
)
