package com.sunny.healthapp.domain.model

enum class BodyBatteryStatus { Charged, Coasting, Depleted, NoData }

data class BodyBatteryPoint(
    /** Minutes since midnight (local zone) of the day this point represents. */
    val minuteOfDay: Int,
    val value: Int,
)

data class BodyBatterySummary(
    val current: Int,
    val morningCharge: Int,
    val status: BodyBatteryStatus,
    val curve: List<BodyBatteryPoint>,
    /** Sub-line breakdown of the morning charge inputs, 0..1 each. */
    val sleepQuality: Double,
    val hrvBalance: Double,
    val rhrBalance: Double,
    val sleepConsistency: Double,
) {
    companion object {
        val Empty = BodyBatterySummary(
            current = 0,
            morningCharge = 0,
            status = BodyBatteryStatus.NoData,
            curve = emptyList(),
            sleepQuality = 0.0,
            hrvBalance = 0.0,
            rhrBalance = 0.0,
            sleepConsistency = 0.0,
        )
    }
}
