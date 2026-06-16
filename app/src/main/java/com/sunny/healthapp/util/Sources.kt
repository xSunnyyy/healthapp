package com.sunny.healthapp.util

/**
 * Maps Health Connect data-origin package names to short, human-friendly
 * labels. Unknown packages fall back to their raw package name.
 */
object Sources {

    fun friendly(packageName: String): String {
        val p = packageName.lowercase()
        return when {
            p.contains("fitbit") -> "Fitbit"
            p.startsWith("com.android.healthconnect.phone") ||
                p == "com.google.android.healthconnect" ||
                p == "com.google.android.platform.health" -> "Android phone (pedometer)"
            p == "com.google.android.apps.fitness" -> "Google Fit"
            p.contains("maestro.companion") ||
                p.startsWith("com.google.android.wearable") -> "Pixel Watch"
            p.startsWith("com.google.android.apps.wear") -> "Wear OS"
            p == "com.google.android.gms" -> "Google Mobile Services"
            p.contains("samsung.android.health") -> "Samsung Health"
            p.contains("garmin") -> "Garmin"
            p.contains("polar") -> "Polar"
            p.contains("whoop") -> "Whoop"
            p.contains("oura") -> "Oura"
            p.contains("strava") -> "Strava"
            p.contains("withings") -> "Withings"
            p.contains("xiaomi") || p.contains("mifit") -> "Mi Fitness"
            else -> packageName
        }
    }

    /** "Fitbit · 699 records" or "com.weird.app · 14 records" if unknown. */
    fun friendlyLine(packageName: String, recordCount: Int): String =
        "${friendly(packageName)}  ·  $recordCount records"
}
