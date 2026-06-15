package com.sunny.healthapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sunny.healthapp.ui.navigation.HealthNavHost
import com.sunny.healthapp.ui.screens.onboarding.OnboardingScreen
import com.sunny.healthapp.ui.theme.HealthAppTheme

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
