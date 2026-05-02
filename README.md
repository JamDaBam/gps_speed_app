# Slide Speed

Minimal native Android app for short GPS-based speed measurements, intended for fun use cases like measuring a ride down a slide.

## Current status

Step 3 is implemented:

- Android project scaffold
- Kotlin + Jetpack Compose app module
- Runtime fine-location permission flow
- GPS/location-services enabled state handling
- High-frequency fused location updates while the app is in the foreground
- Live current speed and current accuracy display in the ready state

Distance/session stats, GNSS satellite status, and session controls are not implemented yet.

## Planned app features

- Current speed in `km/h`
- Distance traveled per session
- Average speed
- Maximum speed
- GPS status bar with fix quality, satellites, and accuracy
- Start, stop, and reset controls

## Build and run

This repository currently contains the app scaffold plus the permission and GPS-readiness prerequisite flow.

To run it:

1. Open the project in Android Studio.
2. Let Android Studio install the required Android SDK and Gradle tooling.
3. Build and run the `app` configuration on an emulator or Android device.

If you prefer CLI builds, generate or add a Gradle wrapper first, then run:

```bash
./gradlew assembleDebug
```

## How to test Step 3

1. Launch the app on a device or emulator with Google Play services or standard Android location settings.
2. On first launch, confirm the app asks for precise location permission.
3. Deny permission once and verify the app keeps showing the permission-required state with a retry button.
4. Deny permission permanently and verify the app offers a button to open app settings.
5. Grant permission with location services turned off and verify the app shows the GPS-disabled state.
6. Turn location services on, return to the app, and verify it switches to the ready state without a restart.
7. Move outdoors or use mock locations and verify the speed value changes from `0 km/h` when location updates arrive.
8. Verify the accuracy label updates in meters and the app stops updating when sent to the background.

## Accuracy note

This app is meant for very short measurement sessions. GPS-based speed and distance can be noisy over short distances, especially with poor satellite visibility or weak accuracy. Later steps will expose satellite and accuracy information so results are easier to judge.
