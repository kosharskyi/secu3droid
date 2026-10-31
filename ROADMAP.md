# Roadmap

## Tablet UI improvements are deferred until after the first stable release

The first polished stable release has shipped. The work below is a post-release
backlog, not a release gate or a commitment to a specific version or date. The
audit was recorded on 2026-10-31.

The original audit found gaps against Android's Adaptive ready (Tier 3) and
Adaptive optimized (Tier 2) guidance. Findings were assigned P2 during the audit;
that priority does not make this backlog mandatory for the first release. The
audit itself was static and did not use a tablet or emulator. Since then, the
settings draft-restoration change has been checked on the Pixel Tablet AVD running
Android 15: all eight `SettingsScreenTest` cases and the retry Activity recreation
integration test passed. The connected test task also packaged the debug APK.
The modal width work has also passed its firmware, diagnostics, and firmware-route
UI checks on that AVD at its effective 1280x800dp display size, including a
landscape side-gutter dismissal check. These focused checks cover the implemented
settings and modal changes; the full tablet compliance matrix below remains
pending.

## Preserve unfinished settings edits across configuration changes

- [x] Preserve retry and fuel-flow draft text through Activity recreation while
  their editors stay open. Invalid input is restored and is not saved
  automatically.
- [x] Cover both editors with saveable-state tests for restoration, confirmation,
  and cancellation. Cover real Activity recreation for retries and verify the
  preference remains unchanged until confirmation.

Affected component: [SettingsScreen.kt](app/src/main/java/org/secu3/android/ui/settings/SettingsScreen.kt).
This finding affects phones as well as tablets.

## Bound modal content width on large windows

- [x] Cap firmware information at 640dp and additional diagnostics options at
  560dp, with available-width behavior on narrow windows.
- [x] Verify modal placement, scrolling, side-gutter dismissal, and short-window
  action reachability on a 1280x800dp Pixel Tablet AVD.

Affected components: [FirmwareRoute.kt](app/src/main/java/org/secu3/android/ui/firmware/FirmwareRoute.kt),
[FirmwareInfoSheet.kt](app/src/main/java/org/secu3/android/ui/firmware/FirmwareInfoSheet.kt),
and [DiagnosticAdditionalDialogContent.kt](app/src/main/java/org/secu3/android/ui/diagnostics/DiagnosticAdditionalDialogContent.kt).

## Keep Dashboard's landscape lock by product decision

Skipped by the maintainer on 2026-10-31. Dashboard is the car's instrument cluster
and is intentionally the only screen that requests a locked landscape
orientation. Removing `SCREEN_ORIENTATION_SENSOR_LANDSCAPE` or introducing a
portrait Dashboard is not part of this roadmap.

Android can ignore the orientation request on large displays. Record that as a
platform compatibility consideration during validation; it does not change the
product decision to retain the request. Preserve the existing Dashboard layout,
orientation tests, system-bar restoration, and keep-screen-on behavior.

See [Dashboard constraints](knowledge/ui/dashboard/DashboardContent.md).

## Adapt Home to available window dimensions

- [x] Select the layout using the measured Home viewport after safe drawing
  insets: retain the phone grid/compact columns, use three tablet columns from
  600dp, and four from 840dp.
- [x] Keep Dashboard across two cells and use the existing compact scrollable
  columns below 480dp viewport height; keep tablet cards tall enough for full
  localized labels as font scale grows.
- [x] Preserve phone card dimensions and menu order, callbacks, dialogs, and
  connection/exit behavior.

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
  and resize through narrow and short split-screen windows. Dashboard retains its
  intentional landscape request; check its platform behavior separately.
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
