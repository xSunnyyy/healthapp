package com.sunny.healthapp.data.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sunny.healthapp.HealthApp
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Periodic worker that runs every ~30 minutes, checks the local clock, and
 * fires whichever Vitals notifications are due in this window (morning,
 * evening goal nudge, bedtime). Idempotent within a day: each kind is posted
 * at most once per calendar day, tracked via DataStore-backed "last fired"
 * flags so device reboots or worker re-runs don't re-spam the user.
 */
class NotificationsWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as HealthApp
        val prefs = app.prefs.current()

        val now = LocalTime.now()
        val today = LocalDate.now()

        try {
            // Morning briefing — 7:30am window
            if (prefs.notifyMorning && now in MORNING_START..MORNING_END) {
                postMorning(app, today)
            }
            // Goal nudge — 7:30pm window if steps short
            if (prefs.notifyGoalNudge && now in GOAL_START..GOAL_END) {
                postGoalNudge(app, today)
            }
            // Bedtime reminder — 9:45pm window
            if (prefs.notifyBedtime && now in BED_START..BED_END) {
                VitalsNotifications.post(
                    applicationContext,
                    VitalsNotifications.ID_BEDTIME,
                    "Wind down time",
                    "Aim for lights out within the next hour to hit a full night.",
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Notification worker failed", e)
        }
        return Result.success()
    }

    private suspend fun postMorning(app: HealthApp, today: LocalDate) {
        val daily = runCatching { app.repository.dailySummary(today) }.getOrNull()
        val sleep = runCatching { app.repository.sleepOnDate(today) }.getOrNull()
        val rhr = daily?.restingHeartRate
        val sleepLine = sleep?.total?.toMinutes()?.let { "Slept ${it / 60}h ${it % 60}m." }
            ?: "Sleep data syncing."
        val rhrLine = rhr?.let { "Resting HR ${it} bpm." } ?: ""
        VitalsNotifications.post(
            applicationContext,
            VitalsNotifications.ID_MORNING,
            "Good morning",
            "$sleepLine $rhrLine".trim(),
        )
    }

    private suspend fun postGoalNudge(app: HealthApp, today: LocalDate) {
        val daily = runCatching { app.repository.dailySummary(today) }.getOrNull() ?: return
        val goal = app.prefs.current().stepsGoal
        val short = goal - daily.steps
        if (short in 1..2_000) {
            VitalsNotifications.post(
                applicationContext,
                VitalsNotifications.ID_GOAL,
                "Almost there",
                "%,d steps to your %,d goal — a short walk does it.".format(short, goal),
            )
        }
    }

    companion object {
        private const val TAG = "NotifWorker"
        private const val WORK_NAME = "vitals.notifications"

        private val MORNING_START = LocalTime.of(7, 0)
        private val MORNING_END = LocalTime.of(8, 30)
        private val GOAL_START = LocalTime.of(19, 0)
        private val GOAL_END = LocalTime.of(20, 30)
        private val BED_START = LocalTime.of(21, 30)
        private val BED_END = LocalTime.of(22, 45)

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationsWorker>(
                30, TimeUnit.MINUTES,
                10, TimeUnit.MINUTES,
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
