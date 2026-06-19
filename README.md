# Hakbang 🚶‍♂️

**Hakbang** ("step" in Filipino) is a privacy-first step/walk counter for Android. All data stays on your device — no internet permission, no account, no cloud sync, no analytics.

## Features

- **Home** — animated circular progress ring showing today's steps vs. your daily goal, live step count, distance (km), calories (kcal), and current streak badge.
- **History** — Daily / Weekly / Monthly bar chart (powered by [Vico](https://github.com/patrykandpatrick/vico)) with total steps, average per day, and best day summary cards.
- **Badges** — milestone badges: First Steps, 5/7/30-day streaks, 10K-in-a-day, 100K and 1M lifetime steps.
- **Settings** — edit daily goal, weight, and stride length; toggle English ↔ Taglish; light/dark theme follows the system (with Material You dynamic color on Android 12+); reset all data with a confirmation dialog.
- **Background counting** — a low-priority foreground service keeps counting steps using the hardware step counter sensor, with a persistent notification.

## Privacy

- No `INTERNET` permission anywhere in the app.
- No account, no login, no cloud backend.
- All step history, settings, and badges are stored locally in Room (SQLite) and Jetpack DataStore.

## Tech Stack

- Kotlin + Jetpack Compose (Material 3, dynamic color)
- MVVM, single-activity architecture
- Room (local database) + DataStore Preferences (settings)
- Compose Navigation, ViewModel, Coroutines/Flow
- Vico for charts
- `Sensor.TYPE_STEP_COUNTER` hardware sensor with reboot-safe cumulative tracking

## How step counting survives reboot and resets at midnight

`Sensor.TYPE_STEP_COUNTER` reports a cumulative count of steps **since the last device reboot** — it resets to 0 whenever the phone restarts. `StepSensorManager` (see `sensor/StepSensorManager.kt`) detects reboots (when a new raw reading is *smaller* than the last one seen) and folds the previous session's total into a persisted `bootOffset`, so `allTimeTotal = bootOffset + rawValue` only ever increases for the lifetime of the install.

Each day's row in Room (`DailySteps`) stores a `baselineSensorValue` — the `allTimeTotal` captured the first time that day was seen. Today's displayed step count is simply `allTimeTotal - baselineSensorValue`, which naturally resets to 0 at midnight when a new day's row is created with a fresh baseline.

## Building

```bash
./gradlew assembleDebug
```

The debug APK is output as `app/build/outputs/apk/debug/Hakbang-v1.0.apk`.

Requires:
- JDK 17
- Android SDK (compileSdk 35, minSdk 26)

## Project Structure

```
app/src/main/java/ph/hakbang/app/
├── data/
│   ├── local/        # Room entities, DAOs, database
│   ├── preferences/   # DataStore-backed UserPreferences
│   └── repository/    # StepRepository — ties sensor + DB + streak/badge logic together
├── sensor/            # StepSensorManager, BootReceiver
├── service/           # StepCounterService (foreground service)
├── ui/
│   ├── theme/         # Material 3 theme, colors, typography
│   ├── navigation/    # Compose Navigation destinations
│   └── screens/       # home, history, badges, settings, permission
└── util/              # StepCalculations, Badge logic, in-app localization strings
```

## Screenshots

_Add screenshots here:_

| Home | History | Badges | Settings |
|------|---------|--------|----------|
| _placeholder_ | _placeholder_ | _placeholder_ | _placeholder_ |

## License

This project is provided as-is for personal/educational use.
