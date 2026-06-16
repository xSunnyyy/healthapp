package com.sunny.healthapp.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.ui.components.Panel
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.EdgeSoft
import com.sunny.healthapp.ui.theme.Ink800
import com.sunny.healthapp.ui.theme.Ink850
import com.sunny.healthapp.ui.theme.MintGlow
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HealthApp
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
    val state by vm.state.collectAsStateWithLifecycle()

    var editing: EditTarget? by remember { mutableStateOf(null) }
    var pickingSource by remember { mutableStateOf(false) }

    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = statusInset + 12.dp, bottom = 60.dp),
    ) {
        // Top bar
        Row(
            modifier = Modifier.padding(horizontal = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                color = TextPrimary,
            )
        }

        Spacer(Modifier.height(20.dp))

        // --- Data source section ---
        SectionLabel("Data source · Fitbit only")
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            Panel(modifier = Modifier.fillMaxWidth()) {
                Text("Reading from", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.activeSource
                        ?.let { com.sunny.healthapp.util.Sources.friendly(it) }
                        ?: "No Fitbit source yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                if (state.activeSource != null) {
                    Text(
                        text = state.activeSource!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
                if (state.prefs.preferredOrigin != null) {
                    Text(
                        "User override",
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Vitals filters Health Connect to your Fitbit data only — no phone " +
                        "pedometer, no Google Fit. If totals look off, run a Force re-sync below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("Change source", onClick = { pickingSource = true })
                    if (state.prefs.preferredOrigin != null) {
                        PillButton("Use Fitbit auto", onClick = { vm.setPreferredOrigin(null) })
                    }
                }
                if (state.availableSources.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Sources seen in the last 30 days",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                    Spacer(Modifier.height(6.dp))
                    state.availableSources.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // --- Phone fill-in toggle ---
        SectionLabel("Smart fill-in")
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            Panel(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Fill steps & distance from phone",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "For hours when your Fitbit wasn't on your wrist, add the " +
                                "phone pedometer's steps & distance. HR, HRV, SpO₂ and " +
                                "sleep always stay strict Fitbit-only.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = state.prefs.phoneFillEnabled,
                        onCheckedChange = { vm.setPhoneFillEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Accent,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Ink800,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // --- Goals ---
        SectionLabel("Daily goals")
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            Panel(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                GoalRow("Steps", "%,d steps".format(state.prefs.stepsGoal), Accent) {
                    editing = EditTarget.Steps(state.prefs.stepsGoal)
                }
                Divider()
                GoalRow("Calories", "%,d kcal".format(state.prefs.caloriesGoal), Accent) {
                    editing = EditTarget.Calories(state.prefs.caloriesGoal)
                }
                Divider()
                GoalRow("Active minutes", "${state.prefs.activeMinutesGoal} min", Accent) {
                    editing = EditTarget.ActiveMinutes(state.prefs.activeMinutesGoal)
                }
                Divider()
                GoalRow("Distance", "%.1f mi".format(state.prefs.distanceGoalMiles), Accent) {
                    editing = EditTarget.Distance(state.prefs.distanceGoalMiles)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // --- Sync ---
        SectionLabel("Sync")
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            Panel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Force a full 365-day re-backfill from Health Connect. " +
                            "Useful after changing the data source or fixing Fitbit's HC settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(14.dp))
                PillButton(
                    label = if (state.lastBackfillRunning) "Syncing…" else "Force full re-sync",
                    onClick = { vm.fullRebackfill() },
                    enabled = !state.lastBackfillRunning,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // --- About ---
        SectionLabel("About")
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            Panel(modifier = Modifier.fillMaxWidth()) {
                Text("Vitals", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("Version 0.2.0 · debug", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Reads Health Connect (the system data store on Android 14+). " +
                            "Fitbit must be allowed to write each data type into Health Connect, " +
                            "or the corresponding screens will show '—'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
    }

    // --- Modals ---
    editing?.let { target ->
        EditValueDialog(
            target = target,
            onDismiss = { editing = null },
            onConfirm = { value ->
                when (target) {
                    is EditTarget.Steps -> vm.setStepsGoal((value.toIntOrNull() ?: target.value).coerceAtLeast(1))
                    is EditTarget.Calories -> vm.setCaloriesGoal((value.toIntOrNull() ?: target.value).coerceAtLeast(1))
                    is EditTarget.ActiveMinutes -> vm.setActiveMinutesGoal((value.toIntOrNull() ?: target.value).coerceAtLeast(1))
                    is EditTarget.Distance -> vm.setDistanceGoalMiles((value.toFloatOrNull() ?: target.value).coerceAtLeast(0.1f))
                }
                editing = null
            }
        )
    }

    if (pickingSource) {
        SourcePickerDialog(
            available = state.availableSources,
            current = state.activeSource,
            override = state.prefs.preferredOrigin,
            onSelect = { pkg ->
                vm.setPreferredOrigin(pkg)
                pickingSource = false
            },
            onDismiss = { pickingSource = false },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun GoalRow(label: String, value: String, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = accent)
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(0.5.dp)
            .background(Color.White.copy(alpha = 0.07f)),
    )
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) Accent.copy(alpha = 0.18f) else Ink800)
            .border(0.6.dp, EdgeSoft, RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) Accent else TextMuted,
        )
    }
}

private sealed class EditTarget(val title: String, val keyboard: KeyboardType, val initial: String) {
    data class Steps(val value: Int) : EditTarget("Daily steps goal", KeyboardType.Number, value.toString())
    data class Calories(val value: Int) : EditTarget("Daily calories goal (kcal)", KeyboardType.Number, value.toString())
    data class ActiveMinutes(val value: Int) : EditTarget("Daily active minutes goal", KeyboardType.Number, value.toString())
    data class Distance(val value: Float) : EditTarget("Daily distance goal (mi)", KeyboardType.Decimal, value.toString())
}

@Composable
private fun EditValueDialog(
    target: EditTarget,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(target.initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Ink850,
        title = { Text(target.title, color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = target.keyboard,
                ),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Save", color = Accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
    )
}

@Composable
private fun SourcePickerDialog(
    available: List<String>,
    current: String?,
    override: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Ink850,
        title = { Text("Choose data source", color = TextPrimary) },
        text = {
            Column {
                if (available.isEmpty()) {
                    Text(
                        "No data sources detected yet. Run a sync first.",
                        color = TextSecondary,
                    )
                }
                available.forEach { pkg ->
                    val isPicked = pkg == (override ?: current)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(pkg) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = com.sunny.healthapp.util.Sources.friendly(pkg),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = pkg,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        if (isPicked) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = "Selected",
                                tint = MintGlow,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = Accent) } },
    )
}
