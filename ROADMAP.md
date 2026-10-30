# Roadmap

## Tablet UI improvements are deferred until after the first stable release

Recorded on 2026-10-31 following a static tablet UI audit. The current priority is
polishing and shipping the first stable release. The work below is a post-release
backlog, not a release gate or a commitment to a specific version or date.

The audit found gaps against Android's Adaptive ready (Tier 3) and Adaptive
optimized (Tier 2) guidance. Findings were assigned P2 during the audit; that
priority does not make this backlog mandatory for the first release. Tablet
runtime behavior and overall compliance have not been verified: no tablet device
or emulator was used, and tests were not run as part of the audit.

## Preserve unfinished settings edits across configuration changes

- [ ] Preserve draft text in `RetryDialog` when the Activity is recreated, including
  rotation and window resizing. It currently uses `remember(initialValue)` and
  resets to the stored value after recreation.
- [ ] Cover both connection retry count and fuel-flow pulse calibration. Verify
  that an unfinished edit survives recreation without being saved automatically,
  and that confirmation and cancellation retain their existing behavior.

Affected component: [SettingsScreen.kt](app/src/main/java/org/secu3/android/ui/settings/SettingsScreen.kt).
This finding affects phones as well as tablets.

## Bound modal content width on large windows

- [ ] Add an appropriate maximum content width for firmware information and the
  additional diagnostics options. Their dialog destinations disable platform
  default width, while their content fills the available width.
- [ ] Verify placement, scrolling, underlying-screen context, and dismiss actions
  on wide tablets and short split-screen windows.

Affected components: [RootNavigation.kt](app/src/main/java/org/secu3/android/ui/RootNavigation.kt),
[FirmwareRoute.kt](app/src/main/java/org/secu3/android/ui/firmware/FirmwareRoute.kt),
[FirmwareInfoSheet.kt](app/src/main/java/org/secu3/android/ui/firmware/FirmwareInfoSheet.kt),
and [DiagnosticAdditionalDialogContent.kt](app/src/main/java/org/secu3/android/ui/diagnostics/DiagnosticAdditionalDialogContent.kt).

## Make Dashboard usable without relying on a landscape lock

- [ ] Revisit the unconditional `SCREEN_ORIENTATION_SENSOR_LANDSCAPE` request and
  support the available tablet window in both orientations.
- [ ] Account for Android 16 ignoring orientation restrictions on large displays
  for this app's current target SDK 36. Verify both platforms that honor the
  request and platforms that ignore it.
- [ ] Update orientation tests alongside the agreed behavior. Preserve system-bar
  restoration, keep-screen-on policy, and connection behavior across recreation.

Affected components: [DashboardRoute.kt](app/src/main/java/org/secu3/android/ui/dashboard/DashboardRoute.kt),
[DashboardContent.kt](app/src/main/java/org/secu3/android/ui/dashboard/DashboardContent.kt),
and [DashboardOrientationTest.kt](app/src/androidTest/java/org/secu3/android/ui/dashboard/DashboardOrientationTest.kt).

## Adapt Home to available window dimensions

- [ ] Replace orientation-only layout selection with a design based on available
  window width and height. The current portrait layout has two columns; the
  landscape layout uses fixed 220dp and 160dp columns.
- [ ] Agree tablet grid density and card proportions before implementation, keeping
  every destination reachable in narrow and short windows.
- [ ] Preserve the accepted phone geometry unless a separate redesign is approved.

Affected components: [HomeScreen.kt](app/src/main/java/org/secu3/android/ui/home/HomeScreen.kt)
and [HomeScreenTest.kt](app/src/androidTest/java/org/secu3/android/ui/home/HomeScreenTest.kt).
See the existing [Home design constraints](knowledge/ui/home/HomeScreen.md).

## Provide keyboard alternatives for sensor reordering

- [ ] Add accessible reorder actions, such as moving an item earlier or later,
  alongside the existing long-press drag gesture.
- [ ] Verify keyboard focus, action feedback, and persisted order for both gauges
  and indicators; retain the existing drag behavior.

Affected components: [SensorGridReorderState.kt](app/src/main/java/org/secu3/android/ui/sensors/SensorGridReorderState.kt)
and [SensorsScreen.kt](app/src/main/java/org/secu3/android/ui/sensors/SensorsScreen.kt).

## Validate tablet support before claiming compliance

- [ ] Run UI checks at 1024x640dp and 1280x800dp, plus their portrait counterparts,
  and resize through narrow and short split-screen windows.
- [ ] Exercise rotation and resizing while a numeric edit or modal is open and
  after scrolling. Verify draft text, selection, navigation, and scroll continuity.
- [ ] Check default and enlarged font sizes, Ukrainian labels, light and dark
  appearance, system insets, touch targets, and keyboard/mouse interaction.
- [ ] Add targeted regression coverage and visually inspect tablet screenshots.

Reuse the adaptive sensor grid, Dashboard width-based layouts, the parameter
forms' 840dp width limit, and existing state-restoration tests. This backlog does
not prescribe a navigation-library migration or a change to connection, protocol,
or persistence contracts. Before implementation, recheck the findings against
current code and submit the scoped work plan required by [AGENTS.md](AGENTS.md).

References: [Adaptive ready (Tier 3)](https://developer.android.com/docs/quality-guidelines/adaptive-app-quality/tier-3),
[Adaptive optimized (Tier 2)](https://developer.android.com/docs/quality-guidelines/adaptive-app-quality/tier-2),
and [Android 16 adaptive layout behavior](https://developer.android.com/about/versions/16/behavior-changes-16#adaptive-layouts).
