# Vitals — Android Health Dashboard

An simple-style health dashboard for Android. Reads from **Health Connect**, so any
device that writes there (including Fitbit) shows up automatically: sleep stages,
heart rate, HRV, SpO₂, respiratory rate, steps, calories, distance, exercise.

## Stack

- Kotlin 2.0, Jetpack Compose, Material 3
- Health Connect (`androidx.health.connect:connect-client`)
- Room + DataStore for caching and prefs
- WorkManager for periodic background sync
- minSdk 28, targetSdk / compileSdk 35, JDK 17

## Screens

- **Home** — daily summary tiles, Body Battery panel, week-of-stats insights
- **Sleep** — time asleep + stages timeline, tappable stage cards with info sheets, night-vitals breakdown
- **Heart rate** — period-aware BPM detail with HR Zones (Resting / Normal / Elevated / High)
- **Activity** — week strip, Goal Progress ring, Calories burned chart, Daily Goals checklist
- **Settings** — data-source picker, daily goals, Smart Fill-in toggle, diagnostics, force re-sync

## Getting your Fitbit data flowing

The Fitbit Air syncs to the Fitbit app, which can write to Health Connect:

1. Install **Health Connect** from the Play Store if you're on Android 13 or lower
   (Android 14+ has it built in).
2. Open the Fitbit app → Profile → App settings → **Health Connect** → enable
   the categories you care about (Steps, Heart rate, Sleep, SpO₂, etc.).
3. Open this app and grant the Health Connect permissions it requests.

Note: Fitbit's Health Connect coverage varies by device, and Fitbit only writes
data forward from when the HC integration is first enabled — there's no historical
backfill. The app handles missing fields gracefully and shows `—` for anything
HC doesn't have.

## Body Battery — how it's calculated

Body Battery is a 0–100 energy reserve that starts with your overnight recovery,
drains during stress and activity, and recharges during calm periods. The math
is built entirely from inputs Health Connect exposes — no weight, height, or age
required.

### Morning charge (run at first HR sample after wake)

```
charge = 100 × (
    0.40 × sleep_quality
  + 0.30 × hrv_balance
  + 0.20 × rhr_balance
  + 0.10 × sleep_consistency
)
```

| Contributor | Formula | Notes |
|---|---|---|
| `sleep_quality` | `(hours / 8) × efficiency × (0.5 + (deep+REM)/total × 0.5)` | Rewards a textbook 8-hour, high-efficiency, restorative night. |
| `hrv_balance` | `((last_night_HRV / 14-day_baseline) − 0.7) / 0.6` clamped 0..1 | Self-referential — you're your own scale. |
| `rhr_balance` | `1 − (last_night_RHR − 14-day_baseline) / 10` clamped 0..1 | Each 1 bpm above baseline costs 10% of this contributor. |
| `sleep_consistency` | `1 − abs(bedtime − 7-day_avg_bedtime) / 120 min` clamped 0..1 | 2-hour bedtime drift = 0. |

If a contributor lacks data (e.g. first launch, no HRV writes from Fitbit), it
defaults to 0.7 so the score doesn't collapse.

### Drain through the day (per 30-min bucket from wake → now)

```
drain = passive + zone + activity
```

| Term | Rate | Notes |
|---|---|---|
| `passive` | 2.5 / hour awake | Background metabolic cost of being awake. |
| `zone` | 0.20 × elevated_minutes + 0.60 × high_minutes | Elevated = 110–149 bpm, High = 150+ bpm. Counted as real clock-minutes, not HR sample rows. |
| `activity` | 0.03 × active_kcal in the bucket | Active calories from Fitbit already encode body composition; we inherit it via Health Connect. |

### Recharge (same buckets)

```
recharge = quiet + nap
```

| Term | Rate | Notes |
|---|---|---|
| `quiet` | 0.10 / quiet minute | A minute counts if every HR sample ≤ 75 bpm AND no steps were recorded that minute. |
| `nap` | 0.40 / minute slept | Any detected sleep session between wake and now contributes. |

### Result

```
battery(t) = clamp( charge − cumulative_drain(t) + cumulative_recharge(t), 0..100 )
```

The hero panel on Home shows `battery(now)` and a small curve from wake to now.
Status word is **Charged** ≥ 70, **Coasting** 40–69, **Depleted** < 40, with a
`NoData` fallback while baselines are still calibrating (needs ~14 nights of
HRV/RHR history to be meaningful).

### Honest caveats

- Without continuous stress data (Fitbit only writes nightly HRV to HC), the
  daytime curve approximates drain from HR zones + activity rather than measuring
  it directly. Same trade-off Whoop's recovery makes.
- The four contributor weights and the drain/recharge rates above are tunable
  in [`BodyBatteryCalculator.kt`](app/src/main/java/com/sunny/healthapp/domain/BodyBatteryCalculator.kt).

## Build

Open in Android Studio (Ladybug or newer) and let it sync, or from the command line:

```sh
# generate the wrapper jar once (the .properties files are committed, the jar is not)
gradle wrapper

# then
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

A stable debug keystore is committed at `keystore/debug.keystore` so every build
(local + GitHub Actions) signs APKs with the same key. That means installing a
new debug APK over an existing one is treated as an update, and the Room cache
of Fitbit history survives.

## Project layout

```
app/src/main/java/com/sunny/healthapp/
├── HealthApp.kt                  # Application: wires HC, Room, prefs, sync
├── MainActivity.kt               # Single Compose activity, onResume sync trigger
├── data/
│   ├── db/                       # Room cache (daily summaries, sleep, HR samples, …)
│   ├── health/                   # HealthConnectManager + HealthRepository
│   ├── prefs/                    # DataStore-backed UserPrefs (goals, toggles)
│   └── sync/                     # HealthSyncManager + HealthSyncWorker
├── domain/
│   ├── BodyBatteryCalculator.kt  # See section above
│   ├── ReadinessCalculator.kt    # Morning readiness composite
│   └── model/                    # DailySummary, SleepSummary, BodyBatterySummary, …
└── ui/
    ├── theme/                    # Dark editorial palette + typography
    ├── navigation/               # 4-tab floating bottom nav
    ├── components/               # Panels, charts, sync indicator, info sheet, …
    └── screens/
        ├── PermissionGate.kt
        ├── onboarding/
        ├── home/
        ├── sleep/
        ├── readiness/
        ├── activity/
        └── settings/
```
