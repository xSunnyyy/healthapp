package com.sunny.healthapp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "vitals_user_prefs")

class UserPrefsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    val prefs: Flow<UserPrefs> = store.data.map { p ->
        UserPrefs(
            onboarded = p[ONBOARDED] ?: false,
            stepsGoal = p[STEPS_GOAL] ?: 10_000,
            caloriesGoal = p[CALORIES_GOAL] ?: 2_500,
            activeMinutesGoal = p[ACTIVE_MIN_GOAL] ?: 30,
            distanceGoalKm = p[DISTANCE_GOAL_KM] ?: 5.0f,
            preferredOrigin = p[PREFERRED_ORIGIN],
        )
    }

    suspend fun current(): UserPrefs = prefs.first()

    suspend fun setOnboarded(value: Boolean) = store.edit { it[ONBOARDED] = value }
    suspend fun setStepsGoal(value: Int) = store.edit { it[STEPS_GOAL] = value }
    suspend fun setCaloriesGoal(value: Int) = store.edit { it[CALORIES_GOAL] = value }
    suspend fun setActiveMinutesGoal(value: Int) = store.edit { it[ACTIVE_MIN_GOAL] = value }
    suspend fun setDistanceGoalKm(value: Float) = store.edit { it[DISTANCE_GOAL_KM] = value }
    suspend fun setPreferredOrigin(value: String?) = store.edit {
        if (value == null) it.remove(PREFERRED_ORIGIN) else it[PREFERRED_ORIGIN] = value
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val STEPS_GOAL = intPreferencesKey("steps_goal")
        val CALORIES_GOAL = intPreferencesKey("calories_goal")
        val ACTIVE_MIN_GOAL = intPreferencesKey("active_min_goal")
        val DISTANCE_GOAL_KM = floatPreferencesKey("distance_goal_km")
        val PREFERRED_ORIGIN = stringPreferencesKey("preferred_origin")
    }
}
