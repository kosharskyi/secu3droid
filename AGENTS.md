# AGENTS.md

Guidance for AI agents working in this repository.

## Scope

This file applies to the whole repository.

## Project Overview

Secu3droid is an Android client for the Secu-3 engine control unit (ECU). The app supports Bluetooth and USB serial connectivity, dashboards/sensors, parameters, diagnostics, logs, firmware/release checks, and local persistence.

## Modules

- `:app` is the only Gradle module declared in `settings.gradle.kts`.
- Main package: `org.secu3.android`.
- Main source set: `app/src/main/java`.
- Test source sets visible in the project: `app/src/test` and `app/src/androidTest`.

## Build System

- Build tool: Gradle wrapper (`./gradlew`).
- Gradle wrapper distribution: Gradle `9.6.1`.
- Android Gradle Plugin: `com.android.application` `9.2.1`.
- Kotlin plugin version: `2.4.0`.
- Dependency versions are managed in `gradle/libs.versions.toml`.
- Repositories are declared in `settings.gradle.kts`: Google, Maven Central, Gradle Plugin Portal, and JitPack.
- Java/Kotlin target compatibility is `17`.

## Android Configuration

From `app/build.gradle.kts`:

- `compileSdk = 37`.
- `targetSdk = 36`.
- `minSdk = 23`.
- `applicationId = "org.secu3.android"`.
- `namespace = "org.secu3.android"`.
- `versionCode = 55`.
- `versionName = "0.18.0"`.
- Build features enabled: ViewBinding, Compose, BuildConfig.
- Release builds use minification and ProGuard/R8 rules from `proguard-rules.pro` and `retrofit2.pro`.
- Room schemas are exported under `app/schemas`.

## Main Stack

- Language: Kotlin.
- UI: Android Fragments/XML/ViewBinding, with Jetpack Compose also enabled and used in the start screen area.
- Navigation: AndroidX Navigation Component with Safe Args.
- Dependency injection: Hilt/Dagger.
- Database: Room.
- Networking: Retrofit, OkHttp, Gson.
- Firebase: Google Services, Analytics, Crashlytics.
- Hardware/connectivity: Android Bluetooth APIs and `usb-serial-for-android`.
- Time/date helper: ThreeTenABP.
- Dashboard visualization: SpeedView.

## Important Directories

- `app/src/main/java/org/secu3/android/connection`: Bluetooth/USB connection handling and foreground connection service.
- `app/src/main/java/org/secu3/android/db`: Room database, DAOs, and entities.
- `app/src/main/java/org/secu3/android/di`: Hilt modules and entry points.
- `app/src/main/java/org/secu3/android/models/packets`: protocol packet models.
- `app/src/main/java/org/secu3/android/network`: API service/manager and network models.
- `app/src/main/java/org/secu3/android/ui`: Activity, fragments, view models, adapters, and UI-specific models.
- `app/src/main/java/org/secu3/android/utils`: preferences, logging, network, file, and utility helpers.
- `app/src/main/res/navigation/main_nav_graph.xml`: main navigation graph.
- `app/src/main/res/layout*`: XML layouts, including tablet/landscape variants.
- `app/src/main/res/values*`: strings, themes, colors, protocol/constants/parameters resources and translations.

## Entry Points

- Application class: `org.secu3.android.Secu3Application`.
- Main activity: `org.secu3.android.ui.MainActivity`.
- Main navigation start destination: `StartScreenFragment`.
- Foreground connection service: `org.secu3.android.connection.SecuConnectionService`.

## Setup Notes

- `app/build.gradle.kts` loads `keystore.properties` only when it exists.
- Debug builds use the default Android debug signing configuration and do not require `keystore.properties`.
- Release builds require `keystore.properties`; keep release signing credentials out of source control.
- `local.properties`, `keystore.properties`, and keystore files should be treated as local machine/configuration data.

## Common Commands

Use the Gradle wrapper from the repository root.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

Notes:

- `connectedDebugAndroidTest` requires a connected Android device or emulator.
- Current CI build command is `./gradlew app:assembleDebug`.
- No project-specific ktlint, detekt, or format command is configured currently.

## Working Rules For Agents

- Do not change files outside the requested scope.
- Preserve existing XML/Fragment/ViewBinding code where that is the local pattern.
- Use Compose for all new UI. The project is being migrated to Compose gradually.
- When changing a screen design or doing a full screen redesign, ask the user first whether the screen should be rewritten in Compose.
- Keep generated files and local configuration files out of edits unless the task explicitly targets them.
- When changing Room entities or DAOs, check whether schema files under `app/schemas` need to be regenerated.
- Do not commit generated Safe Args or KSP outputs. Commit generated Room schemas only.
- When changing navigation destinations or arguments, update `app/src/main/res/navigation/main_nav_graph.xml` and affected Safe Args call sites together.
- When changing Bluetooth, USB, foreground-service, or notification behavior, check the relevant permissions in `app/src/main/AndroidManifest.xml`.
- When changing release/download/network behavior, check ProGuard/R8 rules if Retrofit/Gson model reflection or Firebase behavior could be affected.
- No Kotlin formatter is configured currently. Do not add a formatter unless explicitly requested.

## Unknowns / TODO

- TODO: Confirm the minimum test set required before merging changes.
