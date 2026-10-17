## Appearance migration distinguishes a missing key from a stored value

The new appearance key takes precedence whenever present; unknown or null values resolve to System. Only when it is absent does the legacy boolean apply (`false` = Light, `true` = Dark); when both keys are absent, use System. Reading this fallback is read-only and must not persist either key.

`UserPrefs.appearanceMode` and `SettingsViewModel.readSettings` duplicate this precedence and must remain aligned. The legacy `isDarkTheme` setter cannot override an explicit new key. Persisted values are the stable strings `system`, `light`, and `dark`.
