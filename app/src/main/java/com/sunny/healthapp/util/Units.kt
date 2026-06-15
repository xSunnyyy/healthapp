package com.sunny.healthapp.util

/** Imperial unit helpers — storage stays SI (meters), display goes through these. */
object Units {
    private const val METERS_PER_MILE = 1609.344
    private const val KM_PER_MILE = 1.609344

    fun metersToMiles(meters: Double): Double = meters / METERS_PER_MILE
    fun kmToMiles(km: Double): Double = km / KM_PER_MILE
    fun milesToKm(miles: Double): Double = miles * KM_PER_MILE
    fun milesToMeters(miles: Double): Double = miles * METERS_PER_MILE

    /** "0.27 mi" / "1.4 mi" / "12 mi" — picks a sensible precision for the magnitude. */
    fun formatMiles(meters: Double): String {
        val mi = metersToMiles(meters)
        return when {
            mi < 1.0 -> "%.2f mi".format(mi)
            mi < 10.0 -> "%.1f mi".format(mi)
            else -> "%.0f mi".format(mi)
        }
    }

    fun formatMilesValueOnly(meters: Double): String {
        val mi = metersToMiles(meters)
        return when {
            mi < 1.0 -> "%.2f".format(mi)
            mi < 10.0 -> "%.1f".format(mi)
            else -> "%.0f".format(mi)
        }
    }
}
