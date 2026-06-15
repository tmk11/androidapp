# Cute Camera 🐱 — Face filters

An Android app that opens the camera and draws cute filters that **track your
face** in real time: cat ears + nose + whiskers, a party hat, glasses, and a
puppy. Tap to switch filters or flip between the front and back camera.

## How it works

- **CameraX** drives the live camera preview and feeds frames to an analyzer.
- **ML Kit Face Detection** (on-device, bundled model) finds faces and facial
  landmarks (eyes, nose) in each frame.
- A transparent `FaceOverlayView` maps those landmarks into screen coordinates
  (handling front-camera mirroring + center-crop scaling) and paints the chosen
  filter with `Canvas`.

## Controls

- **Đổi filter ✨** — cycle Cat → Party hat → Glasses → Puppy.
- **Đổi camera 🔄** — switch front / back camera.

## Tech

- Kotlin, `ComponentActivity`, runtime camera permission.
- CameraX 1.3.4, ML Kit `face-detection` 16.1.7 (no Play Services required).
- minSdk 26, targetSdk / compileSdk 34, AGP 8.6.1, Gradle 8.9.

## Project layout

```
app/src/main/
  java/com/example/simpleapp/MainActivity.kt      # camera + permission + binding
  java/com/example/simpleapp/FaceAnalyzer.kt      # CameraX -> ML Kit bridge
  java/com/example/simpleapp/FaceOverlayView.kt   # draws the filters on the face
  res/layout/activity_camera.xml                  # preview + overlay + buttons
  res/values/strings.xml
  AndroidManifest.xml                             # CAMERA permission
```

## Building the APK

### In CI (recommended)

GitHub Actions builds the APK on every push (`.github/workflows/android-build.yml`):

1. **Actions** tab → latest **Build APK** run → download the **`app-debug-apk`**
   artifact, or
2. Grab `app-debug.apk` from the **`debug-latest`** GitHub Release.

Install it on a real device and grant the camera permission to try it.

### Locally

With the Android SDK installed and `ANDROID_HOME` (or `local.properties`) set:

```bash
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

> This app depends on Google's CameraX + ML Kit libraries. It was scaffolded in
> an environment without access to Google's Maven/SDK hosts, so the APK is built
> on GitHub's runners (which can reach them).
