# Pháo Hoa ✨ — Interactive Fireworks

A tiny but delightful Android app written in Kotlin: an interactive fireworks
playground rendered entirely with the Android `Canvas` API — **no external
dependencies**.

## What it does

- **Tap** anywhere → a rocket rises and bursts into a colourful shower of sparks
  (with gravity, fading motion trails, and twinkling embers).
- **Drag** your finger → paint a trail of glowing rainbow sparkles.
- **Shake** the phone → fire a whole salvo of fireworks at once. 🎆
- Background: an animated night sky with twinkling stars and a glowing moon.

Runs full-screen / immersive and keeps the screen on.

## Tech

- Language: Kotlin, single custom `View` (`FireworksView`) + a frame loop driven
  by `Choreographer`.
- Physics & rendering: plain `Canvas`, motion trails via a persistent `Bitmap`
  faded with `PorterDuff.Mode.DST_OUT`.
- Shake detection: `SensorManager` accelerometer.
- minSdk 26, targetSdk / compileSdk 34, AGP 8.6.1, Gradle 8.9 — zero libraries.

## Project layout

```
app/src/main/
  java/com/example/simpleapp/MainActivity.kt    # immersive host activity
  java/com/example/simpleapp/FireworksView.kt   # the whole experience
  res/values/strings.xml                        # app name + on-screen hints
  res/drawable/ + res/mipmap-anydpi-v26/        # firework-burst launcher icon
  AndroidManifest.xml
```

## Building the APK

### In CI (recommended)

GitHub Actions builds the APK on every push (`.github/workflows/android-build.yml`).
After a run finishes:

1. **Actions** tab → latest **Build APK** run → download the **`app-debug-apk`**
   artifact, or
2. Grab `app-debug.apk` from the **`debug-latest`** GitHub Release.

### Locally

With the Android SDK installed and `ANDROID_HOME` (or `local.properties`) set:

```bash
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

> This app was scaffolded in an environment without access to Google's Android
> SDK/Maven hosts, so the APK is built on GitHub's runners instead.
