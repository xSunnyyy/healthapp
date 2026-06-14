# Vitals — Android Health Dashboard

An Oura-style health dashboard for Android. Reads from **Health Connect**, so any
device that writes there (including Fitbit) shows up automatically: sleep stages,
heart rate, HRV, SpO₂, respiratory rate, steps, calories, distance, exercise.

## Stack

- Kotlin 2.0, Jetpack Compose, Material 3
- Health Connect (`androidx.health.connect:connect-client`)
- minSdk 28, targetSdk / compileSdk 35, JDK 17

## Screens

- **Home** — readiness ring hero, last-night sleep score, daily activity rings, vitals
- **Sleep** — score ring, stage timeline, deep/REM/light/awake breakdown, night HR / HRV / SpO₂ / respiration
- **Readiness** — composite score with per-contributor breakdown (sleep, RHR, HRV, sleep balance)
- **Activity** — Apple-style 3-ring (steps / calories / move minutes), distance, floors, weekly steps bar chart

## Getting your Fitbit data flowing

The Fitbit Air syncs to the Fitbit app, which can write to Health Connect:

1. Install **Health Connect** from the Play Store if you're on Android 13 or lower
   (Android 14+ has it built in).
2. Open the Fitbit app → Profile → App settings → **Health Connect** → enable
   the categories you care about (Steps, Heart rate, Sleep, SpO₂, etc.).
3. Open this app and grant the Health Connect permissions it requests.

Note: Fitbit's Health Connect coverage varies by device. Steps, HR, sleep
(including stages on most modern trackers) and SpO₂ are usually exposed. HRV
and detailed "readiness inputs" are sometimes missing — the app handles missing
fields gracefully and shows `—` for unavailable metrics.

## Build

Open in Android Studio (Ladybug or newer) and let it sync, or from the command line:

```sh
# generate the wrapper jar once (the .properties files are committed, the jar is not)
gradle wrapper

# then
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

## Project layout

```
app/src/main/java/com/sunny/healthapp/
├── HealthApp.kt                  # Application: wires HealthConnectManager + Repository
├── MainActivity.kt               # Single Compose activity, splash, edge-to-edge
├── data/health/                  # Health Connect plumbing
│   ├── HealthPermissions.kt      # Read permission set
│   ├── HealthConnectManager.kt   # Availability, permission contract, raw reads
│   └── HealthRepository.kt       # Domain-shaped reads (daily summary, sleep, readiness)
├── domain/
│   ├── ReadinessCalculator.kt    # Heuristic readiness score
│   └── model/                    # DailySummary, SleepSummary, ReadinessSummary
└── ui/
    ├── theme/                    # Dark Oura-inspired palette, typography
    ├── navigation/               # 4-tab bottom nav
    ├── components/               # ScoreRing, ActivityRings, MetricCard, SleepStagesBar
    └── screens/
        ├── PermissionGate.kt     # Wraps every screen, drives HC permission flow
        ├── home/                 # Dashboard
        ├── sleep/                # Sleep detail
        ├── readiness/            # Readiness detail
        └── activity/             # Activity detail + weekly chart
```

## Readiness is heuristic

Oura's formula is proprietary. `ReadinessCalculator` here is a directional 0–100
composite from: last-night sleep score, resting HR vs. 7-day baseline, HRV vs.
7-day baseline, and 7-day sleep duration. Tune freely.
