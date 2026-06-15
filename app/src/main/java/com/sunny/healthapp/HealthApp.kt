package com.sunny.healthapp

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.sunny.healthapp.data.db.HealthDatabase
import com.sunny.healthapp.data.health.HealthConnectAvailability
import com.sunny.healthapp.data.health.HealthConnectManager
import com.sunny.healthapp.data.health.HealthRepository
import com.sunny.healthapp.data.sync.HealthSyncManager
import com.sunny.healthapp.data.sync.HealthSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HealthApp : Application(), Configuration.Provider {

    lateinit var healthConnect: HealthConnectManager
        private set
    lateinit var database: HealthDatabase
        private set
    lateinit var syncManager: HealthSyncManager
        private set
    lateinit var repository: HealthRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        healthConnect = HealthConnectManager(this)
        database = HealthDatabase.get(this)
        syncManager = HealthSyncManager(healthConnect, database)
        repository = HealthRepository(healthConnect, database)

        // Schedule periodic background sync and kick an immediate one if HC is ready.
        HealthSyncWorker.schedule(this)
        appScope.launch {
            try {
                if (healthConnect.availability == HealthConnectAvailability.Installed &&
                    healthConnect.hasAllPermissions()
                ) {
                    syncManager.syncAll(force = false)
                }
            } catch (e: Exception) {
                Log.w("HealthApp", "Initial sync failed", e)
            }
        }
    }
}
