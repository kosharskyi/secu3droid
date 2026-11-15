## Preference rows continue to call the owning route

The shared settings rows are presentation-only. Toggle, selection, and retry actions must continue through the existing callbacks so preference persistence and route-owned dialogs remain the source of state. Keep resource-key test tags on actual checkbox controls and preserve appearance selection callbacks.

## Pending preference loading and failed writes must not imply stored defaults

The Settings and Sensors Settings routes wait for their first preference snapshot before exposing editable controls. Keep that readiness separate from the preference defaults owned by `UserPrefs`. Main Settings keeps its appearance dialog open until the write succeeds. The shared choice dialog only reports a selection and never decides when to close. Sensors Settings closes its choice dialog immediately after the selection callback, matching its existing behavior.

## Fuel consumption calibration is a persisted instrument preference

The pulse-per-litre value is stored in the same DataStore `Settings` snapshot as the other application preferences (default 16,000; inclusive range 1,000–32,000, matching Manager's `FFFConst`). Its numeric dialog stays open until the preference write succeeds; range parsing and writes belong to `UserPrefs`, while the route only forwards callbacks.

## Injection timing convention is selected in main Instruments settings

The single `injection_timing_mode` preference is owned by the main Settings > Instruments section. Sensors, Dashboard, and Fuel Injection parameter rows all read the same observed `UserPrefs.Settings` value. Keep Sensors Settings limited to its old-view toggle and gauge style; parameter editors must use the mode captured when the editor opens.

## Numeric settings drafts survive recreation but end with the editor

Connection retry and fuel-flow inputs use saveable Compose state so text, including invalid partial input, survives Activity recreation while that dialog remains open. The draft is not a preference and is written only through a valid confirmation. Dismissing the dialog drops it; reopening initializes from the current settings value. A failed preference write keeps the editor and its draft open.

## Analytics consent changes are applied by the collection owner

The Settings ViewModel forwards consent to `UsageAnalytics`, which closes the SDK before persisting revocation and prevents unrelated stale settings emissions from reopening collection during that write. The Settings row only presents the stored snapshot and forwards the toggle.

## The forum link follows the last page as the topic grows

The About link uses `start=2147483647` because phpBB resolves an offset beyond the number of posts to the last page. All locales use the same destination so the link continues to reach the latest page as replies are added.

## About keeps the Settings entry on the back stack

Pop the About destination to return to the existing Settings entry. It owns the saved scroll position across the round trip and Activity recreation.
