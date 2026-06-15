package com.sunny.healthapp.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sunny.healthapp.HealthApp
import java.util.concurrent.TimeUnit

class HealthSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as HealthApp
        return try {
            if (!app.healthConnect.hasAllPermissions()) {
                Log.i(TAG, "Permissions not granted yet; skipping sync")
                return Result.success()
            }
            app.syncManager.syncAll(force = false)
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Sync failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "HealthSyncWorker"
        private const val WORK_NAME = "vitals.health.sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<HealthSyncWorker>(
                30, TimeUnit.MINUTES,
                10, TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
            Log.i(TAG, "Periodic sync scheduled (every ~30 min)")
        }
    }
}
