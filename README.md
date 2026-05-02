# Slide Speed

Minimal native Android app for short GPS-based speed measurements, intended for fun use cases like measuring a ride down a slide.

## Current status

Step 1 is implemented:

- Android project scaffold
- Kotlin + Jetpack Compose app module
- Single placeholder screen
- Basic README and project structure

GPS permissions, location tracking, session stats, and GNSS status handling are not implemented yet.

## Planned app features

- Current speed in `km/h`
- Distance traveled per session
- Average speed
- Maximum speed
- GPS status bar with fix quality, satellites, and accuracy
- Start, stop, and reset controls

## Build and run

This repository currently contains the Android source scaffold only.

To run it:

1. Open the project in Android Studio.
2. Let Android Studio install the required Android SDK and Gradle tooling.
3. Build and run the `app` configuration on an emulator or Android device.

If you prefer CLI builds, generate or add a Gradle wrapper first, then run:

```bash
./gradlew assembleDebug
```

## Accuracy note

This app is meant for very short measurement sessions. GPS-based speed and distance can be noisy over short distances, especially with poor satellite visibility or weak accuracy. Later steps will expose satellite and accuracy information so results are easier to judge.
