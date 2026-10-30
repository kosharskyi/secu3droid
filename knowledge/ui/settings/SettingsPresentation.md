## Preference rows continue to call the owning route

The shared settings rows are presentation-only. Toggle, selection, folder, retry, and About actions must continue through the existing callbacks so preference persistence and route-owned dialogs remain the source of state. Keep resource-key test tags on actual checkbox controls and preserve appearance/delimiter selection callbacks.

## Pending preference loading and failed writes must not imply stored defaults

The Settings and Sensors Settings routes wait for their first preference snapshot before exposing editable controls. Keep that readiness separate from the preference defaults owned by `UserPrefs`. Main Settings keeps its appearance and delimiter dialogs open until the write succeeds; the shared choice dialog only reports a selection and never decides when to close. Sensors Settings closes its choice dialog immediately after the selection callback, matching its existing behavior.

## The export picker delegates URI grants before saving the selection

The route ignores canceled results and results without a URI. The ViewModel saves the directory only after `FileHelper` accepts the grant, preserving the previously saved destination if Android rejects it.

## Fuel consumption calibration is a persisted instrument preference

The pulse-per-litre value is stored in the same DataStore `Settings` snapshot as the other application preferences (default 16,000; inclusive range 1,000–32,000, matching Manager's `FFFConst`). Its numeric dialog stays open until the preference write succeeds; range parsing and writes belong to `UserPrefs`, while the route only forwards callbacks.
