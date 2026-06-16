package com.sunny.healthapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sunny.healthapp.data.sync.SyncStatus
import com.sunny.healthapp.ui.navigation.HealthNavHost
import com.sunny.healthapp.ui.screens.onboarding.OnboardingScreen
import com.sunny.healthapp.ui.theme.HealthAppTheme
import java.time.Duration
import java.time.Instant

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthAppTheme {
                Root(application as HealthApp)
            }
        }
    }

    /**
     * Resume = "user is looking at the app". If the last successful sync was
     * more than 5 minutes ago, kick a new one. Keeps HR data within 5 min of
     * whatever Fitbit has written to Health Connect without spamming sync on
     * every config change.
     */
    override fun onResume() {
        super.onResume()
        val app = application as HealthApp
        val last = (app.syncManager.status.value as? SyncStatus.Done)?.at
        val stale = last == null || Duration.between(last, Instant.now()).toMinutes() >= 5
        if (stale) {
            Log.i("MainActivity", "onResume → triggering sync (last sync: $last)")
            app.triggerManualSync(force = false)
        }
    }
}

@Composable
private fun Root(app: HealthApp) {
    val prefs by app.prefs.prefs.collectAsState(initial = null)
    val isOnboarded = prefs?.onboarded
    when (isOnboarded) {
        null -> { /* hold blank — DataStore still warming up */ }
        false -> OnboardingScreen(onDone = { /* state flips via DataStore */ })
        true -> HealthNavHost()
    }
}
