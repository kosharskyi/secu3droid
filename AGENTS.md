# AGENTS.md

Guidance for AI agents working in this repository.

## Scope

This file applies to the whole repository.

## Project Overview

Secu3droid is an Android client for the Secu-3 engine control unit (ECU). The app supports Bluetooth and USB serial connectivity, dashboards/sensors, parameters, diagnostics, logs, firmware/release checks, and local persistence.

## UI Architecture

The project uses Jetpack Compose for UI, Material3 for Compose components, and Navigation Compose for navigation. Do not add Compose Material2 widgets or Google Material view/XML libraries. Compose Material icon artifacts are allowed for the existing `androidx.compose.material.icons` namespace. XML resources may provide platform-only Activity/window themes. Follow `knowledge/compose-migration.md` for screen and navigation conventions.

## Important Directories

- `app/src/main/java/org/secu3/android/connection`: Bluetooth/USB connection handling and foreground connection service.
- `app/src/main/java/org/secu3/android/db`: Room database, DAOs, and entities.
- `app/src/main/java/org/secu3/android/di`: Hilt modules and entry points.
- `app/src/main/java/org/secu3/android/models/packets`: protocol packet models.
- `app/src/main/java/org/secu3/android/network`: API service/manager and network models.
- `app/src/main/java/org/secu3/android/utils`: preferences, logging, network, file, and utility helpers.
- `app/src/main/java/org/secu3/android/ui`: Activity, Compose routes/screens, view models, adapters, and UI-specific models.
- `app/src/main/res/values*`: strings, themes, colors, protocol/constants/parameters resources and translations.

## Entry Points

- Application class: `org.secu3.android.Secu3Application`.
- Main activity: `org.secu3.android.ui.MainActivity`.
- Navigation start destination: `START_ROUTE` in `RootNavigation`.
- Foreground connection service: `org.secu3.android.connection.SecuConnectionService`.

## Working Rules For Agents

- Do not change files outside the requested scope.
- Use Compose for all new UI and Navigation Compose for new destinations and flows. Do not introduce new Fragment/XML destinations or Safe Args usage.
- Use `androidx.compose.material3` for all Compose UI widgets and theming. Do not add `androidx.compose.material` widget artifacts or Google Material view/XML libraries; existing `androidx.compose.material.icons` artifacts are allowed for icon compatibility.
- When rewriting an existing screen or flow, migrate its UI and navigation to Compose. Preserve behavior in legacy code during scoped fixes.
- Keep generated files and local configuration files out of edits unless the task explicitly targets them. Never commit signing credentials or keystore files.
- When changing Room entities or DAOs, check whether schema files under `app/schemas` need to be regenerated.
- Do not commit generated Safe Args or KSP outputs. Commit generated Room schemas only.
- When migrating existing navigation, update or remove affected entries in `app/src/main/res/navigation/main_nav_graph.xml` and Safe Args call sites together.
- When changing Bluetooth, USB, foreground-service, or notification behavior, check the relevant permissions in `app/src/main/AndroidManifest.xml`.
- When changing release/download/network behavior, check ProGuard/R8 rules if Retrofit/Gson model reflection or Firebase behavior could be affected.
- Do not add a formatter unless explicitly requested.

## Agent Collaboration Workflow

- The lead agent handles planning, complex architectural decisions, and code review when the change warrants it. Delegate implementation to Luna.
- Luna implements the task, reviews her own diff, runs the relevant tests and build checks, and fixes issues discovered during those checks within the task's scope.
- Do not automatically review all code after every Luna task. For simple changes whose relevant checks passed, use Luna's completion report to close the task.
- The lead agent joins code review for complex, risky, or architecturally important changes, or when Luna reports uncertainty, failed tests or builds, or parts that could not be verified. Keep the review focused on the relevant changes and risks. Follow any explicit user request for review.
- After completing a task, Luna provides a short report covering:
  - What changed.
  - Which tests and build checks passed.
  - What could not be verified and why.
  - Any remaining risks, doubts, or unresolved issues; explicitly state when there are none.
