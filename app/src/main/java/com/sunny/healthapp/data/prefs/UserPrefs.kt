package com.sunny.healthapp.data.prefs

data class UserPrefs(
    val onboarded: Boolean = false,
    val stepsGoal: Int = 10_000,
    val caloriesGoal: Int = 2_500,
    val activeMinutesGoal: Int = 30,
    /** Daily distance goal in miles. Stored as miles for display parity. */
    val distanceGoalMiles: Float = 3.0f,
    /** When non-null, sync filters everything through this exact package name. */
    val preferredOrigin: String? = null,
)
