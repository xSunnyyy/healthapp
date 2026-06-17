package com.sunny.healthapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sunny.healthapp.ui.components.AppBackground
import com.sunny.healthapp.ui.components.FloatingNavBar
import com.sunny.healthapp.ui.components.NavItem
import com.sunny.healthapp.ui.screens.activity.ActivityScreen
import com.sunny.healthapp.ui.screens.home.HomeScreen
import com.sunny.healthapp.ui.screens.readiness.ReadinessScreen
import com.sunny.healthapp.ui.screens.settings.SettingsScreen
import com.sunny.healthapp.ui.screens.sleep.SleepScreen
import com.sunny.healthapp.ui.screens.trends.HrvTrendScreen
import com.sunny.healthapp.ui.theme.ActivityGreen
import com.sunny.healthapp.ui.theme.ReadinessLilac
import com.sunny.healthapp.ui.theme.SleepBlue
import com.sunny.healthapp.ui.theme.WarmPeach

enum class HealthDest(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val accent: Color,
) {
    Home("home", "Home", Icons.Outlined.Home, WarmPeach),
    Sleep("sleep", "Sleep", Icons.Outlined.Bedtime, SleepBlue),
    Readiness("readiness", "Readiness", Icons.Outlined.Favorite, ReadinessLilac),
    Activity("activity", "Activity", Icons.AutoMirrored.Outlined.DirectionsRun, ActivityGreen),
}

@Composable
fun HealthNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val items = HealthDest.entries.map {
        NavItem(it.route, it.label, it.icon, it.accent)
    }

    AppBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = HealthDest.Home.route,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(HealthDest.Home.route) {
                    HomeScreen(onNavigate = { route ->
                        if (route == "settings") {
                            navController.navigate("settings")
                        } else if (HealthDest.entries.any { it.route == route }) {
                            navController.navigate(route) {
                                popUpTo(HealthDest.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    })
                }
                composable(HealthDest.Sleep.route) { SleepScreen() }
                composable(HealthDest.Readiness.route) { ReadinessScreen() }
                composable(HealthDest.Activity.route) { ActivityScreen() }
                composable("settings") {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
                composable("hrv_trend") {
                    HrvTrendScreen(onBack = { navController.popBackStack() })
                }
            }
            // Hide the floating nav on full-screen routes that need their own back-stack feel.
            if (currentRoute !in setOf("settings", "hrv_trend")) {
                FloatingNavBar(
                    items = items,
                    selectedKey = backStack?.destination?.hierarchy?.firstOrNull { node ->
                        items.any { it.key == node.route }
                    }?.route,
                    onSelect = { item ->
                        if (currentRoute != item.key) {
                            navController.navigate(item.key) {
                                popUpTo(HealthDest.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
