# Simple App

A minimal Android application written in Kotlin. It shows a greeting, a counter,
and a button that increments the counter on each tap.

## Project layout

```
app/
  src/main/
    java/com/example/simpleapp/MainActivity.kt   # single screen
    res/layout/activity_main.xml                 # UI layout
    res/values/strings.xml                       # text resources
    res/drawable/ + res/mipmap-anydpi-v26/       # adaptive launcher icon
    AndroidManifest.xml
  build.gradle.kts                               # module config
build.gradle.kts / settings.gradle.kts           # project config
.github/workflows/android-build.yml              # builds the APK in CI
```

- Language: Kotlin
- minSdk 26, targetSdk / compileSdk 34
- Android Gradle Plugin 8.6.1, Gradle 8.9
- No external dependencies (uses the Android framework directly)

## Building the APK

### In CI (recommended)

This repository builds the APK automatically with GitHub Actions on every push
(`.github/workflows/android-build.yml`). After a run finishes:

1. Open the **Actions** tab → the latest **Build APK** run.
2. Download the **`app-debug-apk`** artifact, or
3. Grab `app-debug.apk` from the **`debug-latest`** GitHub Release.

### Locally

You need the Android SDK installed and `ANDROID_HOME` (or a `local.properties`
with `sdk.dir=...`) configured, then:

```bash
./gradlew assembleDebug
```

The APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

> Note: this app was scaffolded in an environment without access to Google's
> Android SDK/Maven hosts, so the APK is built on GitHub's runners instead.
