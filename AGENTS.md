# AGENTS.md

Guidance for AI agents working in this repository.

## Scope

This file applies to the whole repository.

## Project Overview

Secu3droid is an Android client for the Secu-3 engine control unit (ECU). The app supports Bluetooth and USB serial connectivity, dashboards/sensors, parameters, diagnostics, logs, firmware/release checks, and local persistence.

## UI Architecture

The project uses Jetpack Compose for UI, Material3 for Compose components, and Navigation Compose for navigation. Do not add Compose Material2 widgets or Google Material view/XML libraries. Compose Material icon artifacts are allowed for the existing `androidx.compose.material.icons` namespace. XML resources may provide platform-only Activity/window themes. Follow `knowledge/compose-migration.md` for screen and navigation conventions.

Business logic must not be implemented in UI layers, including Activities, Compose routes/screens, ViewModels, adapters, and UI-specific state holders or controllers. Business rules, connection/session lifecycle decisions, protocol behavior, persistence/network policies, and domain error classification belong to the domain, data, or core components that own those responsibilities.

UI code may map domain state to presentation (including localized messages and styling), manage UI-only interaction/navigation state, and forward user actions to the owning components. ViewModels expose presentation state and coordinate those calls; they must not reconstruct domain/session history or duplicate business rules to infer outcomes. If presentation needs a distinction missing from a shared contract, extend the owning component's typed state/result and update its consumers and tests within the approved scope.

Connection managers own connection establishment, reconnection, cancellation, transport-resource cleanup, connection status, and the common incoming packet stream. They must not know about screens, routes, ViewModels, or how packets are delivered to or displayed by particular UI consumers.

Transport classes (`Connection`, `BtConnection`, and `UsbConnection`) expose their own states, outcomes, and packets. Coordinate logging and other consumer side effects in the connection manager using those existing contracts. Do not add transport methods, callbacks, or events solely to trigger a consumer action. Extend the shared transport contract only when it lacks a fact about the transport itself.

Do not put display-state ownership into a connection manager: no latest-packet caches, per-packet-type state streams, replay, sampling, filtering, or reset policies introduced for screen consumption. Packet-derived consumer state and its presentation lifecycle belong outside the connection manager. Shared consumer behavior must follow the class/helper ownership rules below rather than expanding the manager's responsibilities.

## Class and Helper Organization

- Prefer the simplest concrete implementation that satisfies current requirements. Add an abstraction only when it provides a concrete, present benefit that outweighs the extra types, indirection, and maintenance. This applies to interfaces, base classes, wrappers, enum/sealed hierarchies, and helper or coordinator classes.
- An interface with a single implementation added merely to abstract that implementation is an anti-pattern. Do not introduce one solely for dependency injection, architectural layering, or a hypothetical future implementation; concrete classes can be injected directly.
- Reuse existing state and contracts instead of adding parallel representations, pass-through result types, or metadata that production consumers do not need. During refactoring, remove redundant abstractions and duplicated state rather than moving the same complexity into more classes.
- Do not create standalone files that contain only top-level helper functions. Keep helper logic in the class that owns the responsibility, or in a dedicated class when extraction is justified. Compose UI functions continue to follow the existing screen and component conventions.
- Non-Compose helper functions and the runtime collections or catalogs they operate on must not be top-level declarations, including `private` declarations. Adding a data/model class to the same file does not exempt those helpers or collections from class ownership and dependency-injection rules. Keep the data and operations together in their owning class.
- When shared helper behavior would otherwise be implemented as a static utility, use an injectable `@Singleton` class with an `@Inject` constructor and inject it into its consumers. Do not replace dependency injection with a Kotlin `object`, global helper functions, or manual singleton access.
- General-purpose Kotlin extension functions that are reusable across unrelated components must be defined in `app/src/main/java/org/secu3/android/utils/Extentions.kt`. Before adding an extension, check this file and search the project for equivalent behavior; reuse the existing implementation instead of duplicating it. Do not scatter reusable extensions across screens, classes, or new utility files. Such extensions are an explicit exception to the top-level helper and injectable utility rules in this section. Keep component-specific behavior, business rules, mutable state, and logic requiring injected dependencies in their owning classes.
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

- Before implementing a non-trivial task, write and present an implementation specification and wait for the user's explicit approval. Include the problem, scope, expected behavior and edge cases, affected files and components, shared contracts and consumers, risks, acceptance criteria, and validation steps. For trivial tasks, get approval for a brief description of the change. Read-only investigation is allowed before approval. Do not edit project files or delegate implementation before approval.
- Work only within the approved scope. If the solution requires expanding scope or introducing additional architectural or core changes, revise the specification and obtain approval before proceeding.
- Treat changes to core modules (including connection handling, packet/protocol infrastructure, persistence, and shared utilities) as project-wide changes. Inspect all consumers and call sites, prefer a common solution that accounts for them, and update affected consumers and tests together. Screen-specific methods or infrastructure are allowed only when the functionality is unnecessary for other consumers or a shared solution would overcomplicate them. Explain that tradeoff in the specification and obtain approval before implementation.
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
- Name release version-bump commits `Build #<versionCode>`, using the new Android `versionCode` from `app/build.gradle.kts` (for example, `Build #60` when the new code is `60`).
- Do not add a formatter unless explicitly requested.

## Agent Collaboration Workflow

For non-trivial tasks, the main agent coordinates the workflow and makes final decisions.

1. Ask `planner` to inspect the codebase and propose a minimal plan. The main agent resolves architectural choices, writes the implementation specification, and presents it in the chat for the user's approval. Save it as a project file when the user requests a persistent specification.
2. Give `implementer` one bounded handoff with the original requirements and the approved specification. The specification defines what to implement and how to verify it. The implementer reviews the diff, runs relevant checks, and reports the changes, validation results, deviations, and unresolved issues.
3. Give `simplicity_reviewer` the approved specification and resulting diff for independent review using the rules below.
4. Assign justified blocking findings to `implementer` for fixes within the approved scope. If a fix requires expanding scope, update the specification and request approval before proceeding.

Skip `planner` for trivial tasks. Handle them directly or use one specialist if their expertise is needed. Use `luna_compose` for targeted UI work, `luna_protocol` for packet parsing, serialization, or parameter mapping, and `luna_runtime` for connection or session lifecycle, queues, logging, or file operations. Use `secu_mapper` only when code ownership or data flow is unclear and `secu_android_qa` only when reproduction or targeted Android validation is needed.

Keep agent handoffs focused and avoid duplicate work or routine review passes. The main agent joins code review for complex, risky, or architecturally important changes, or when validation fails or leaves material uncertainty. Follow explicit user requests for review.

Subagents return one concise final report. Include the result, relevant validation, and any blockers or deviations. Keep code excerpts and command output to the lines needed to verify a finding or diagnose a failure. Send an interim message only when a decision or action is needed to continue, or when the handoff explicitly requests an update.

## Engineering Principles

1. Prefer the simplest solution that fully satisfies the requirements.
2. Follow existing project conventions before introducing new patterns.
3. Do not introduce abstractions for a single implementation without a concrete need.
4. Do not design for hypothetical future requirements.
5. Avoid unnecessary interfaces, wrappers, factories, managers, and configuration layers.
6. Minimize the number of files, dependencies, and architectural layers affected.
7. Do not refactor unrelated code.
8. Every new abstraction must solve a demonstrated problem.
9. Prefer deleting unnecessary code over adding more code.
10. When two solutions are equally correct, choose the one with lower maintenance complexity.

Before implementing, ask:
- Can this be solved by modifying existing code?
- Is each new abstraction necessary today?
- Am I solving a real requirement or an imagined future problem?


## Simplicity Review

After completing a non-trivial implementation,
delegate a review to `simplicity_reviewer`.

Provide:
- Original task requirements.
- Approved implementation specification, or the approved brief for a trivial task.
- Implementation diff.
- Relevant architectural context.

The reviewer must independently assess whether
the implementation introduces unnecessary complexity.

Do not invoke the reviewer for trivial changes
unless explicitly requested.

Handle findings as follows:
- BLOCKER: Address before completing the task.
- WARNING: Address when the simplification is
  clearly beneficial and low-risk.
- SUGGESTION: Optional; do not automatically implement.

Limit independent review to two passes total, including at most one follow-up after blocking findings are addressed. The main agent verifies any remaining fixes and decides how to resolve outstanding findings within the approved scope.


## Project knowledge

Technical deep-dive notes go in `knowledge/`, organized to mirror the package structure (e.g. `knowledge/ui/<screen>/<component>.md`, `knowledge/domain/<area>/...`).

- **Before** touching a component, check for a corresponding file in `knowledge/`. If one exists, read it in full — it captures constraints that are not visible from the code alone.
- **After** changing a component, update (or create) its knowledge file to reflect any new invariants, gotchas, or cross-component contracts.
- Only document what would **surprise** a competent reader of the code: hidden constraints and invariants, non-obvious cross-component contracts, workarounds for specific bugs or platform quirks (with the reason), and sentinel values or index conventions. Do not describe what the code does, repeat architecture covered here, or record anything derivable by reading the class in isolation.
- Format: one `##` heading per insight, written as a short statement of the rule. Follow with a brief explanation, and a code snippet only if it aids understanding.
