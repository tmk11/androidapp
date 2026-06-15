# Doanh Thu Nail 💅 — Nail salon revenue tracker

An Android app for a nail salon to record daily revenue per technician
(**Ha, David, Tu, Anh**) in **Euro (€)**. Data is stored locally on the device
with SQLite, so it persists between sessions and works fully offline.

## Features

- **Per-day entry screen**
  - Pick the day (defaults to today).
  - For each technician: an amount field + **Thêm** (Add) to record a payment.
    Multiple entries per day are summed automatically.
  - Live per-technician totals and a grand **day total**.
  - List of each entry made that day, with delete (to fix mistakes).
- **Report screen** — totals per technician for **today**, **this month**, and
  **all time**, each with a grand total.
- Amounts are entered/edited in cents internally and shown as Euro (e.g. `12,50 €`).

## Tech

- Kotlin, `AppCompatActivity`, Material 3 components (cards, text fields, buttons).
- Local storage with `SQLiteOpenHelper` (no backend, no internet permission).
- minSdk 26, targetSdk / compileSdk 34, AGP 8.6.1, Gradle 8.9.

## Project layout

```
app/src/main/
  java/com/example/simpleapp/
    MainActivity.kt     # day entry screen
    ReportActivity.kt   # totals per technician (today / month / all time)
    NailDb.kt           # SQLite storage + Entry model + technician list
    Money.kt            # Euro formatting + robust input parsing (cents)
    Dates.kt            # day-key + display date helpers
  res/layout/           # screens and reusable item layouts
  res/values/           # strings, colors, Material 3 theme
  AndroidManifest.xml
```

To change the technicians, edit `TECHS` in `NailDb.kt`.

## Building the APK

GitHub Actions builds the APK on every push
(`.github/workflows/android-build.yml`):

1. **Actions** tab → latest **Build APK** run → download the **`app-debug-apk`**
   artifact, or
2. Grab `app-debug.apk` from the **`debug-latest`** GitHub Release.

Locally (with the Android SDK installed): `./gradlew assembleDebug` →
`app/build/outputs/apk/debug/app-debug.apk`.

> The app uses Google's AndroidX / Material libraries, so it is built on GitHub's
> runners (which can reach Google's Maven), not in the offline scaffold sandbox.
