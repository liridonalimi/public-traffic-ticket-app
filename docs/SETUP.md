# Native Android Environment Setup

This project is prepared as a native Android app using Kotlin and Jetpack Compose.

## Required tools

Install these on the Mac:

1. Android Studio
2. JDK 17 or newer
3. Android SDK Platform 35
4. Android SDK Build Tools

The easiest path is to install Android Studio, then open this folder:

```text
/Users/ljiridon.aljimi/Documents/public_traffic_ticket_app
```

Android Studio should detect the Gradle project and download the Android Gradle Plugin and Kotlin plugin automatically.

## First run

1. Open the project in Android Studio.
2. Let Gradle sync finish.
3. Create an Android emulator or connect a real Android tablet.
4. Run the `app` configuration.

## Product direction

The first milestone is one driver tablet that can:

1. Start a shift.
2. Select a bus and route.
3. Track GPS.
4. Sell cash tickets offline.
5. Sync tickets to the server later.
6. Show next stop and stop-request state.

The code is already split into:

- `ui`: driver and passenger screens
- `domain`: bus, route, stop, shift, and ticket models
- `data`: local storage and sync layer
- `device`: GPS, printer, and stop-button integrations
