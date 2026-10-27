## Preference rows continue to call the owning route

The shared settings rows are presentation-only. Toggle, selection, folder, retry, and About actions must continue through the existing callbacks so preference persistence and route-owned dialogs remain the source of state. Keep resource-key test tags on actual checkbox controls and preserve immediate appearance/delimiter selection behavior.

## Pending preference loading and failed writes must not imply stored defaults

The Settings and Sensors Settings routes wait for their first preference snapshot before exposing editable controls. Keep that readiness separate from the preference defaults owned by `UserPrefs`. A dialog closes only after its write succeeds; a storage failure leaves the edit visible and reports the error through the existing message queue.
