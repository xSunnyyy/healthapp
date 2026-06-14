package com.sunny.healthapp.domain.model

data class ReadinessContribution(
    val label: String,
    val score: Int,
)

data class ReadinessSummary(
    val score: Int,
    val contributions: List<ReadinessContribution>,
)
