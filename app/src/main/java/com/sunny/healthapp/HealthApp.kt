package com.sunny.healthapp

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.sunny.healthapp.data.db.HealthDatabase
import com.sunny.healthapp.data.health.HealthConnectAvailability
import com.sunny.healthapp.data.health.HealthConnectManager
import com.sunny.healthapp.data.health.HealthRepository
import com.sunny.healthapp.data.prefs.UserPrefsRepository
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
    lateinit var prefs: UserPrefsRepository
        private set
    lateinit var syncManager: HealthSyncManager
        private set
    lateinit var repository: HealthRepository
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun triggerManualSync(force: Boolean = false) {
        appScope.launch {
            try {
                if (healthConnect.hasAllPermissions()) syncManager.syncAll(force = force)
            } catch (e: Exception) {
                Log.w("HealthApp", "Manual sync failed", e)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        healthConnect = HealthConnectManager(this)
        database = HealthDatabase.get(this)
        prefs = UserPrefsRepository(this)
        syncManager = HealthSyncManager(healthConnect, database, prefs)
        repository = HealthRepository(healthConnect, database)

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
