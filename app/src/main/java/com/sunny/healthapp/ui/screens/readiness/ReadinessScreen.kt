package com.sunny.healthapp.ui.screens.readiness

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.domain.model.ReadinessContribution
import com.sunny.healthapp.ui.components.GlassCard
import com.sunny.healthapp.ui.components.ScoreRing
import com.sunny.healthapp.ui.screens.PermissionGate
import com.sunny.healthapp.ui.theme.ReadinessLilac

@Composable
fun ReadinessScreen() {
    PermissionGate {
        val app = LocalContext.current.applicationContext as HealthApp
        val vm: ReadinessViewModel = viewModel(factory = ReadinessViewModel.factory(app))
        val state by vm.state.collectAsStateWithLifecycle()
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ReadinessLilac, strokeWidth = 2.dp)
            }
        } else {
            Content(state)
        }
    }
}

@Composable
private fun Content(state: ReadinessState) {
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = statusInset + 16.dp, bottom = 130.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                "Readiness".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "How recovered are you?",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScoreRing(score = state.readiness?.score ?: 0, label = "Readiness", color = ReadinessLilac)
            Spacer(Modifier.height(16.dp))
            Text(
                text = verdict(state.readiness?.score ?: 0),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(28.dp))
        GlassCard(
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            tint = ReadinessLilac,
        ) {
            Text(
                "Contributors".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            state.readiness?.contributions?.forEachIndexed { i, c ->
                ContributionRow(c)
                if (i != state.readiness.contributions.lastIndex) {
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun ContributionRow(c: ReadinessContribution) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = c.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = c.score.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar(progress = c.score / 100f, color = ReadinessLilac)
    }
}

@Composable
private fun ProgressBar(progress: Float, color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp),
    ) {
        drawLine(
            color = Color.White.copy(alpha = 0.10f),
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width * progress.coerceIn(0f, 1f), size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Round,
        )
    }
}

private fun verdict(score: Int): String = when {
    score >= 85 -> "Primed."
    score >= 70 -> "Steady."
    score >= 55 -> "Take it easy."
    else -> "Recover."
}
