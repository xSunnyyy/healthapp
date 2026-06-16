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
    /**
     * When true, sync fills steps & distance from the phone pedometer for
     * hours where Fitbit recorded zero (e.g. watch wasn't worn). HR, HRV,
     * SpO2 and sleep stay strict Fitbit-only regardless.
     */
    val phoneFillEnabled: Boolean = false,
)
