# AGENTS.md

Guidance for AI agents working in this repository.

## Scope

This file applies to the whole repository.

## Project Overview

Secu3droid is an Android client for the Secu-3 engine control unit (ECU). The app supports Bluetooth and USB serial connectivity, dashboards/sensors, parameters, diagnostics, logs, firmware/release checks, and local persistence.

## UI Architecture

The project uses Jetpack Compose for UI, Material3 for Compose components, and Navigation Compose for navigation. Do not add Compose Material2 widgets or Google Material view/XML libraries. Compose Material icon artifacts are allowed for the existing `androidx.compose.material.icons` namespace. XML resources may provide platform-only Activity/window themes. Follow `knowledge/compose-migration.md` for screen and navigation conventions.

Business logic must not be implemented in UI layers, including Activities, Compose routes/screens, ViewModels, adapters, and UI-specific state holders or controllers. Business rules, connection/session lifecycle decisions, protocol behavior, persistence/network policies, and domain error classification belong to the domain, data, or core components that own those responsibilities.

UI code may map domain state to presentation (including localized messages and styling), manage UI-only interaction/navigation state, and forward user actions to the owning components. ViewModels expose presentation state and coordinate those calls; they must not reconstruct domain/session history or duplicate business rules to infer outcomes. If presentation needs a distinction missing from a shared contract, extend the owning component's typed state/result and update its consumers and tests within the approved plan.

## Class and Helper Organization

- Do not create standalone files that contain only top-level helper functions. Keep helper logic in the class that owns the responsibility, or in a dedicated class when extraction is justified. Compose UI functions continue to follow the existing screen and component conventions.
- When shared helper behavior would otherwise be implemented as a static utility, use an injectable `@Singleton` class with an `@Inject` constructor and inject it into its consumers. Do not replace dependency injection with a Kotlin `object`, global helper functions, or manual singleton access.
- Do not introduce a new class solely to hold one method used in a single place. Keep that method in the consuming class, normally as a private method, while respecting the business-logic boundaries above.
- A group of methods may be extracted into a separate class when they share a cohesive responsibility or context, even if that class currently has only one consumer. Base extraction on the existing responsibility and usage, not hypothetical future reuse.

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

- Before implementation, present a detailed work plan and wait for the user's explicit approval. Include the problem, proposed approach, affected files and components, behavior changes, shared contracts and consumers, risks, and validation steps. Read-only investigation to prepare the plan is allowed; do not edit project files or delegate implementation before approval.
- Work only within the approved plan. If the solution requires expanding scope or introducing architectural/core changes not covered by that plan, stop and submit a revised plan for approval before proceeding.
- Treat changes to core modules (including connection handling, packet/protocol infrastructure, persistence, and shared utilities) as project-wide changes. Inspect all consumers and call sites, prefer a common solution that accounts for them, and update affected consumers and tests together. Screen-specific methods or infrastructure are allowed only when the functionality is unnecessary for other consumers or a shared solution would overcomplicate them. Explain that tradeoff in the work plan and obtain approval before implementation.
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

- Prefer the project agent profile whose role fits the task; use the general-purpose Luna implementation agent only when no profile fits. Keep the existing plan approval requirement in force before implementation.
- Route clear UI fixes to `luna_compose`, packet parsing/serialization or parameter mapping to `luna_protocol`, and connection/session lifecycle, queues, logging, or file operations to `luna_runtime`. Use `secu_mapper` only when code ownership or data flow is unclear. Use `secu_android_qa` separately only when reproduction or targeted Android validation is needed; it is not an automatic second test pass.
- Do not launch every role for a task. The lead agent handles planning and architectural choices, assigns one bounded implementation handoff, and joins review for complex, risky, or uncertain changes. Avoid duplicate routine reviews after relevant checks pass.
- Luna implements the task, reviews her own diff, runs the relevant tests and build checks, and fixes issues discovered during those checks within the task's scope.
- Do not automatically review all code after every Luna task. For simple changes whose relevant checks passed, use Luna's completion report to close the task.
- The lead agent joins code review for complex, risky, or architecturally important changes, or when Luna reports uncertainty, failed tests or builds, or parts that could not be verified. Keep the review focused on the relevant changes and risks. Follow any explicit user request for review.
- After completing a task, Luna provides a short report covering:
  - What changed.
  - Which tests and build checks passed.
  - What could not be verified and why.
  - Any remaining risks, doubts, or unresolved issues; explicitly state when there are none.

## Project knowledge

Technical deep-dive notes go in `knowledge/`, organized to mirror the package structure (e.g. `knowledge/ui/<screen>/<component>.md`, `knowledge/domain/<area>/...`).

- **Before** touching a component, check for a corresponding file in `knowledge/`. If one exists, read it in full — it captures constraints that are not visible from the code alone.
- **After** changing a component, update (or create) its knowledge file to reflect any new invariants, gotchas, or cross-component contracts.
- Only document what would **surprise** a competent reader of the code: hidden constraints and invariants, non-obvious cross-component contracts, workarounds for specific bugs or platform quirks (with the reason), and sentinel values or index conventions. Do not describe what the code does, repeat architecture covered here, or record anything derivable by reading the class in isolation.
- Format: one `##` heading per insight, written as a short statement of the rule. Follow with a brief explanation, and a code snippet only if it aids understanding.
