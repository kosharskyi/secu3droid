## Diagnostic labels follow family resources without changing catalog indices

The output rows and test-signal selector use family-specific label arrays paired by position with the domain's indexed output catalog. SECU-3i labels use `IGN_O1` through `IGN_O5`, and display `ECF0` as `ECF`; the selector displays `O2SH_O` with the letter O. The selector prepends `NONE` at index zero. Keep resource order aligned with each family catalog so these display names never alter protocol channel IDs. Output test tags continue to use enum names.

## Hidden output rows must not filter the selector catalog

The output list may hide `TACH_O` while its direction option is disabled, but the channel catalog retains its full family order. Filtering is presentation-only so channel IDs after TACH remain stable.

## Missing input and a zero-valued packet have different presentations

Diagnostics inputs receive a nullable packet while firmware data is unavailable. Keep that null state visible as unavailable; zeroes and false bits are legitimate readings once a packet exists. Digital input state uses a compact colored text badge and matching accessibility state description. These are read-only ECU indicators.

## Knock-signal graphs use session samples and clip only their visual scale

The KS_1 and KS_2 charts share paired, timestamped samples owned by `DiagnosticsSession`, so switching tabs or opening additional options does not interrupt history. The visible X range is ten seconds ending at the newest sample; values outside 0–5 V are clipped only while drawing, leaving numeric readings unchanged. Hide the trace whenever the observed connection or fresh input is unavailable, even if a prior state snapshot still contains samples.

## Output auto-test disables overlapping controls on every diagnostics tab

Only show Start as enabled for a ready session with generator channel `NONE`; show Stop while the session reports an active test. During a run, mask generator editors and the additional-options action on the inputs tab, and rely on the shared output readiness rule for output switches. Keep the timer state in the graph-scoped session so pager changes and dialog navigation do not reset its lifecycle.

## Output switches remain user-command controls

The output checkboxes represent ECU commands. Preserve firmware and readiness gates, accessibility behavior, and invoke callbacks only from user interaction; do not turn them into passive status indicators.

Both row and checkbox use the domain state's `canSetOutput` rule. The screen also masks readiness when the observed connection or input is unavailable, even if a previous domain snapshot is still visible.

## Firmware capability labels stay reactive

The route collects the connection manager's nullable firmware state so a reply while diagnostics is open updates input labels and capability presentation. Clearing firmware removes stale capability. Show ADD_IO3 only outside SECU-3T, and treat ADD_IO4 as unavailable unless family and TPIC8101 capability are both known and enabled; a supported value of `0.000 V` is still a real reading.

## Keep additional options inside the native dialog's measured bounds

Cap the options surface at 560dp and the outer content, including its 16dp margins, at 592dp. The native Compose Dialog uses the measured content bounds for outside-tap dismissal, so a full-width wrapper would consume taps in the side gutters. Keep the options and actions in the existing vertically scrollable, 640dp-capped column for short windows.

## Bluetooth retries retain the diagnostics graph but invalidate its input

Bluetooth `Reconnecting` retains the current diagnostics graph and ViewModel. The session still disables controls and clears its old input, then re-enters diagnostics after `Connected` and requires fresh input before enabling controls. USB loss and terminal Bluetooth disconnection return to Start and remove the graph. Automatic Home navigation applies only on Start, so recovery cannot replace a retained feature graph. Integration fixtures must establish their initial transport and let Home navigation finish before entering diagnostics.
