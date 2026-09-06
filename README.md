# OpenFit

A privacy-first, open-source **lifting tracker + macro (nutrition) tracker in one app**, for Android,
optimized for [GrapheneOS](https://grapheneos.org/). Built with Kotlin and Jetpack Compose.

OpenFit merges two open-source projects — **OpenLift** (strength training) and **MacroTracker**
(photo-based nutrition tracking) — into a single offline-first app. It has no accounts, no cloud,
no trackers, no ads. All of your training and nutrition data lives locally on your device (Room /
SQLite), and you can export/import a single backup JSON file when you want to move it.

## Features

### Lifting tracker (from OpenLift)
- **Pre-loaded exercise catalog** — every major barbell and dumbbell exercise, grouped by muscle
  group (Chest, Back, Shoulders, Quads, Hamstrings, Glutes, Calves, Biceps, Triceps, Forearms,
  Core, Full Body).
- **Workout sessions** — create a workout, add multiple exercises, and log sets (weight + reps)
  with warm-up marking.
- **Rest timer** — automatically starts a countdown after each logged set; configurable default
  rest time in Settings and adjustable per-session.
- **Last-session placeholders** — when you add an exercise to a new workout, the set count,
  weights, and reps are pre-filled from your most recent session for that exercise.
- **Most-used on top** — your most frequently used exercises appear at the top of the picker.
- **Progress graphs** — estimated 1-rep max (Epley) charted over time per exercise using
  [Vico](https://github.com/patrykandpatrick/vico), with best/latest/change stats.
- **Workout summary** — duration, volume, sets/reps, best e1RM, and a per-exercise breakdown.
- **Custom exercises** — create your own exercises inline.

### Nutrition tracker (from MacroTracker)
- **Photo-based macro logging** — snap a meal photo and an AI vision model (any OpenAI-compatible
  endpoint you configure) drafts the calories, protein, carbs, and fat.
- **Review before saving** — confirm or edit the AI draft, or re-use a recent meal.
- **Today's intake dashboard** — totals vs. your daily goals with progress bars.
- **Meal history & gallery** — every meal grouped by day, with a photo gallery.
- **Daily goals** — configurable calorie/protein/carbs/fat targets.

### Shared
- **Unified history** — workouts and meals in one timeline.
- **Units** — store weights canonically in kg, display and enter in either kg or lb.
- **Local backup** — export/import the full database (workouts + meals) as a JSON file via the
  system file picker.

## Privacy & GrapheneOS

- **No trackers, analytics, or ads.** Only AndroidX, Room, Coil, OkHttp, and Vico.
- **Local-only storage** in SQLite via Room with `allowBackup=false` and backup excluded from
  cloud/device-transfer rules — your data doesn't leave the device except through your explicit
  export.
- **Network access is opt-in and user-coupled.** The app declares `INTERNET` permission **only** to
  call the AI-vision endpoint *you* configure in Settings. Photos and data are sent **only** to that
  endpoint and nowhere else; the workout features work fully offline with zero network calls.
- **Manual DI**, no heavy frameworks, R8 + resource shrinking for a small, fast APK.
- **F-Droid-friendly** MIT-licensed build configuration.

## Getting started

### Prerequisites

- JDK 17+
- Android SDK (Android 15 / API 35, build-tools 35)

### Build

```bash
./gradlew :app:assembleRelease
# or
./gradlew :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/`.

### Tests

```bash
./gradlew :app:testDebugUnitTest
```

The unit tests cover the e1RM calculators (Epley/Brzycki), unit conversion, progress aggregation,
exercise catalog parsing, full Room repository behavior for workouts and meals (including the
placeholder-seeding logic) using Robolectric on the JVM — no emulator required.

## Architecture

- **UI** — Jetpack Compose + Material 3, single-activity with Navigation Compose and a bottom bar.
- **State** — ViewModel + StateFlow, `viewModelFactory` wired to an `AppContainer` (manual DI).
- **Data** — two Room databases (`openfit_workout.db` and `openfit_macros.db`) with their own DAOs
  and repositories, an `ExerciseSeeder` that loads `assets/exercises.json`, and a `BackupManager`
  that backs up both in one JSON file.
- **AI vision** — `llm/LlmClient` builds the request for your configured endpoint and parses the
  structured JSON drafts.
- **Domain** — pure functions for `OneRepMax`, `UnitConverter`, `TimeFormatter`, `Stats`,
  `ProgressCalculator`, and `PickerOrder` (all unit-tested).

```
app/src/main/java/dev/openfit/app/
├── OpenFitApplication.kt
├── MainActivity.kt
├── di/AppContainer.kt
├── data/                 (repositories, seeder, backup)
│   ├── local/            (workout Room entities, DAOs, database)
│   └── macro/            (meal entity, DAO, database, models)
├── llm/                  (AI vision client + draft DTOs)
├── domain/               (testable pure logic)
└── ui/
    ├── navigation/ theme/ components/
    ├── home/ history/ workout/ picker/ summary/ progress/
    ├── macros/           (dashboard, capture, review, gallery)
    └── settings/
```

## License

MIT. See [LICENSE](LICENSE).
