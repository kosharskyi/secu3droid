# AGENTS.md

Guidance for AI agents working in this repository.

## Scope

This file applies to the whole repository.

## Project Overview

Secu3droid is an Android client for the Secu-3 engine control unit (ECU). The app supports Bluetooth and USB serial connectivity, dashboards/sensors, parameters, diagnostics, logs, firmware/release checks, and local persistence.

## UI Architecture

The project should use Jetpack Compose for UI and Navigation Compose for navigation. Existing Fragment/XML destinations are transitional. Follow `knowledge/compose-migration.md` for screen and navigation conventions.

## Important Directories

- `app/src/main/java/org/secu3/android/connection`: Bluetooth/USB connection handling and foreground connection service.
- `app/src/main/java/org/secu3/android/db`: Room database, DAOs, and entities.
- `app/src/main/java/org/secu3/android/di`: Hilt modules and entry points.
- `app/src/main/java/org/secu3/android/models/packets`: protocol packet models.
- `app/src/main/java/org/secu3/android/network`: API service/manager and network models.
- `app/src/main/java/org/secu3/android/ui`: Activity, fragments, view models, adapters, and UI-specific models.
- `app/src/main/java/org/secu3/android/utils`: preferences, logging, network, file, and utility helpers.
- `app/src/main/res/navigation/main_nav_graph.xml`: legacy Fragment navigation graph.
- `app/src/main/res/layout*`: remaining XML layouts.
- `app/src/main/res/values*`: strings, themes, colors, protocol/constants/parameters resources and translations.

## Entry Points

- Application class: `org.secu3.android.Secu3Application`.
- Main activity: `org.secu3.android.ui.MainActivity`.
- Current legacy navigation start destination: `StartScreenFragment`.
- Foreground connection service: `org.secu3.android.connection.SecuConnectionService`.

## Working Rules For Agents

- Do not change files outside the requested scope.
- Use Compose for all new UI and Navigation Compose for new destinations and flows. Do not introduce new Fragment/XML destinations or Safe Args usage.
- When rewriting an existing screen or flow, migrate its UI and navigation to Compose. Preserve behavior in legacy code during scoped fixes.
- Keep generated files and local configuration files out of edits unless the task explicitly targets them. Never commit signing credentials or keystore files.
- When changing Room entities or DAOs, check whether schema files under `app/schemas` need to be regenerated.
- Do not commit generated Safe Args or KSP outputs. Commit generated Room schemas only.
- When migrating existing navigation, update or remove affected entries in `app/src/main/res/navigation/main_nav_graph.xml` and Safe Args call sites together.
- When changing Bluetooth, USB, foreground-service, or notification behavior, check the relevant permissions in `app/src/main/AndroidManifest.xml`.
- When changing release/download/network behavior, check ProGuard/R8 rules if Retrofit/Gson model reflection or Firebase behavior could be affected.
- Do not add a formatter unless explicitly requested.
