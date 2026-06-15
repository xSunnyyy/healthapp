package com.sunny.healthapp.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.data.health.HealthConnectAvailability
import com.sunny.healthapp.data.health.HealthPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HealthApp
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf<Boolean?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = app.healthConnect.permissionContract,
    ) { result ->
        val ok = result.containsAll(HealthPermissions.READ)
        granted = ok
        if (ok) {
            scope.launch(Dispatchers.IO) {
                runCatching { app.syncManager.syncAll(force = true) }
            }
        }
    }

    LaunchedEffect(Unit) {
        val ok = app.healthConnect.hasAllPermissions()
        granted = ok
        if (ok) {
            scope.launch(Dispatchers.IO) {
                runCatching { app.syncManager.syncAll(force = false) }
            }
        }
    }

    when {
        app.healthConnect.availability != HealthConnectAvailability.Installed ->
            HealthConnectMissing(app.healthConnect.availability)
        granted == true -> content()
        granted == false -> RequestPermissions { launcher.launch(HealthPermissions.READ) }
        else -> Box(Modifier.fillMaxSize())
    }
}

@Composable
private fun HealthConnectMissing(state: HealthConnectAvailability) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.HealthAndSafety,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Text(
                text = when (state) {
                    HealthConnectAvailability.ProviderUpdateRequired ->
                        "Health Connect needs an update from the Play Store."
                    else -> "Health Connect isn't available on this device."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun RequestPermissions(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Outlined.HealthAndSafety,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Text(
                text = "Connect your Fitbit data",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Vitals reads sleep, heart rate, activity and more from Health Connect. Make sure the Fitbit app is set to write to Health Connect.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("Grant Health Connect access")
            }
        }
    }
}
