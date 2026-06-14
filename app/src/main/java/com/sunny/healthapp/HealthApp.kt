package com.sunny.healthapp

import android.app.Application
import com.sunny.healthapp.data.health.HealthConnectManager
import com.sunny.healthapp.data.health.HealthRepository

class HealthApp : Application() {
    lateinit var healthConnect: HealthConnectManager
        private set
    lateinit var repository: HealthRepository
        private set

    override fun onCreate() {
        super.onCreate()
        healthConnect = HealthConnectManager(this)
        repository = HealthRepository(healthConnect)
    }
}
