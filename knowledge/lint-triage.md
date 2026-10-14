# Android lint triage

`./gradlew :app:lintDebug` currently completes with 95 warnings. The preference migration and Compose cleanup removed warnings caused by avoidable code issues, including API-gated permission constants, empty `onCleared` super calls, composable naming, configuration-unaware resource access, private AppCompat resources, an out-of-order `Modifier` parameter, and a URI conversion suggestion.

The remaining warnings are grouped below. They were left for focused follow-up because resolving them would require broader SDK, resource, localization, or UI asset decisions than this maintenance change:

- **76 `UnusedResources`**: legacy colors, drawables, strings, and styles. Review them against dynamic resource use and app history before deleting.
- **7 `Typos`**: `PWM1`, `MAP1`, `VE1`, and `INPUT1` are ECU terminology and identifiers.
- **3 `PluralsCandidate`**: two copyright strings and a diagnostic summary; the copyright values are years and the diagnostic string contains multiple quantities.
- **3 `TypographyOther`**: legacy `(c)` text in existing string resources.
- **3 `IconColors` and 1 `IconMissingDensityFolder`**: legacy menu icon assets; changing them requires visual asset review.
- **1 `OldTargetApi`**: target SDK 36; raising it needs platform behavior testing.
- **1 `AndroidGradlePluginVersion`**: lint's wrapper-version advisory; no build-tool update was made as part of this change.

The remaining warnings are informational or need product/resource validation. They do not fail the lint task.
