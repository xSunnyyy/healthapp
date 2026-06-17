package com.sunny.healthapp.domain.model

enum class AnomalySeverity { Low, Medium, High }

enum class AnomalyKind { RestingHrUp, HrvDown, ShortSleep, LongSleep, StepsLow, StepsHigh }

data class AnomalyInsight(
    val kind: AnomalyKind,
    val title: String,
    val description: String,
    val severity: AnomalySeverity,
)
