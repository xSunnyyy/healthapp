package com.sunny.healthapp.ui.screens.onboarding

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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunny.healthapp.HealthApp
import com.sunny.healthapp.ui.components.AppBackground
import com.sunny.healthapp.ui.theme.Accent
import com.sunny.healthapp.ui.theme.EdgeSoft
import com.sunny.healthapp.ui.theme.Ink800
import com.sunny.healthapp.ui.theme.Lavender
import com.sunny.healthapp.ui.theme.MintGlow
import com.sunny.healthapp.ui.theme.Sunflare
import com.sunny.healthapp.ui.theme.TextMuted
import com.sunny.healthapp.ui.theme.TextPrimary
import com.sunny.healthapp.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val body: String,
    val icons: List<Pair<ImageVector, Color>>,
)

private val pages = listOf(
    OnboardingPage(
        title = "Welcome to Vitals.",
        subtitle = "A calm view of your day",
        body = "Vitals turns your Fitbit data into a clean editorial summary of " +
                "your sleep, heart, and activity — sourced directly from Android " +
                "Health Connect.",
        icons = listOf(
            Icons.Outlined.Bedtime to Lavender,
            Icons.Outlined.MonitorHeart to Sunflare,
            Icons.Outlined.Bolt to MintGlow,
        ),
    ),
    OnboardingPage(
        title = "Connect your Fitbit.",
        subtitle = "Two settings inside the Fitbit app",
        body = "1. Open the Fitbit app → Profile → app settings → Health Connect.\n" +
                "2. Toggle on Steps, Heart rate, Sleep, Active energy and Distance.\n\n" +
                "Without this, Vitals (and any other app reading Health Connect) " +
                "won't see your numbers.",
        icons = listOf(
            Icons.Outlined.Bluetooth to Accent,
            Icons.Outlined.HealthAndSafety to MintGlow,
        ),
    ),
    OnboardingPage(
        title = "Grant Health Connect access.",
        subtitle = "Read-only · stays on your device",
        body = "Vitals never sends your data anywhere — every read goes through " +
                "Android Health Connect on the phone you're holding. On the next " +
                "screen, allow each data type you'd like surfaced.",
        icons = listOf(
            Icons.Outlined.HealthAndSafety to Accent,
            Icons.Outlined.Favorite to com.sunny.healthapp.ui.theme.Crimson,
        ),
    ),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as HealthApp
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    AppBackground {
        Column(modifier = Modifier.fillMaxSize().padding(top = statusInset, bottom = navInset)) {
            // Skip button top-right
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                if (pagerState.currentPage < pages.lastIndex) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.clickable {
                            scope.launch { app.prefs.setOnboarded(true) }
                            onDone()
                        },
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                PageContent(pages[page])
            }

            // Page dots
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                pages.indices.forEach { i ->
                    val selected = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) Accent else TextMuted.copy(alpha = 0.6f)),
                    )
                }
            }

            // CTA
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                val ctaLabel = when (pagerState.currentPage) {
                    pages.lastIndex -> "Get started"
                    else -> "Next"
                }
                CtaButton(label = ctaLabel) {
                    if (pagerState.currentPage == pages.lastIndex) {
                        scope.launch { app.prefs.setOnboarded(true) }
                        onDone()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        // Pre-warm so the first scroll is smooth
        pagerState.scrollToPage(0)
    }
}

@Composable
private fun PageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            page.icons.forEach { (icon, color) ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.18f))
                        .border(0.6.dp, color.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp, lineHeight = 42.sp),
            color = TextPrimary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = Accent,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
            color = TextSecondary,
        )
    }
}

@Composable
private fun CtaButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Accent)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = Color.Black,
            textAlign = TextAlign.Center,
        )
    }
}
