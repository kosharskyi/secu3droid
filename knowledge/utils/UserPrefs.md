## Appearance migration distinguishes a missing key from a stored value

The new appearance key takes precedence whenever present; unknown or null values resolve to System. Only when it is absent does the legacy boolean apply (`false` = Light, `true` = Dark); when both keys are absent, use System. Reading this fallback is read-only and must not persist either key.

`UserPrefs.appearanceMode` and `SettingsViewModel.readSettings` duplicate this precedence and must remain aligned. The legacy `isDarkTheme` setter cannot override an explicit new key. Persisted values are the stable strings `system`, `light`, and `dark`.

## Sensor gauge style defaults to the precise arc without repairing storage

The stable `sensor_gauge_style` values are `current`, `precise_arc`, and `thin_needle`. Missing or unknown values resolve to `precise_arc` in memory without writing a replacement. Explicitly stored `current` and `thin_needle` values remain selected; the older-sensor and per-gauge numeric preferences remain independent.

## Injection timing display stores only a presentation convention

The stable `injection_timing_mode` values are `btdc_720`, `atdc_720`, `btdc_360`, and `atdc_360`; missing or unknown values read as the desktop-compatible `btdc_720` default without repairing storage. This preference transforms only the old Sensors readout. ECU timing values and parameter editing remain in their original 0…720 representation.

## Retired sensor column preference is intentionally ignored

Older installs may still contain the `columns_count` SharedPreferences key. No code reads, rewrites, or removes it; the Sensors UI derives columns from its current content width, and leaving the stale key untouched avoids an unnecessary preference migration.

## Bluetooth selection identity is the stored address

`bluetoothDeviceAddress` is the only Bluetooth identity used to validate, start, or reconnect a connection. `bluetoothDeviceName` is retained as display metadata only; older installs that have only the name must select their device again instead of guessing among bonded devices with duplicate names.
