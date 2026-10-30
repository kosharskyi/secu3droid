## Appearance migration distinguishes a missing key from a stored value

The new appearance key takes precedence whenever present; unknown or null values resolve to System. Only when it is absent does the legacy boolean apply (`false` = Light, `true` = Dark); when both keys are absent, use System. Reading this fallback is read-only and must not persist either key.

`UserPrefs` maps this precedence into its canonical `Settings.appearanceMode`; UI consumers must use that value. The legacy boolean is migrated only for reading older installations. Persisted appearance values are the stable strings `system`, `light`, and `dark`.

## DataStore owns settings reads and writes

The active values from `${packageName}_preferences` and `app_shared_prefs` migrate through `PreferencesMigrations` into `user_preferences.preferences_pb` and `app_preferences.preferences_pb`. Its finite allowlists include only keys still consumed by production; `columns_count` remains in the old file. Existing DataStore values win if migration is retried. Both stores are excluded from backup through the `datastore/` file path while the old SharedPreferences exclusion policy remains in place.

Settings consumers use the single `Settings` snapshot so multi-key observations stay coherent, and writes are suspending. Long-lived observers use `observeSettings` to report an I/O failure and retry centrally; one-shot reads and writes still propagate failures to their caller. Do not add synchronous accessors or treat failed reads as defaults.

## Sensor gauge style defaults to the precise arc without repairing storage

The stable `sensor_gauge_style` values are `current`, `precise_arc`, and `thin_needle`. Missing or unknown values resolve to `precise_arc` in memory without writing a replacement. Explicitly stored `current` and `thin_needle` values remain selected; the older-sensor and per-gauge numeric preferences remain independent.

## Injection timing display stores only a presentation convention

The stable `injection_timing_mode` values are `btdc_720`, `atdc_720`, `btdc_360`, and `atdc_360`; missing or unknown values read as the `btdc_720` default without repairing storage. This preference transforms only the old Sensors readout. ECU timing values and parameter editing remain in their original 0…720 representation.

## Fuel consumption calibration is independent of the ECU injector parameter

`fuel_flow_pulses_per_litre` is an application preference matching Manager's `FFFConst`: missing or invalid stored values resolve in memory to 16,000, and explicit writes accept only 1,000–32,000. It controls local sensor-flow presentation and logging; do not derive it from or write it to the ECU injector packet.

## Retired sensor column preference is intentionally ignored

Older installs may still contain the `columns_count` SharedPreferences key. No code reads, rewrites, or removes it; the Sensors UI derives columns from its current content width, and leaving the stale key untouched avoids an unnecessary preference migration.

## Bluetooth selection identity is the stored address

`bluetoothDeviceAddress` is the only Bluetooth identity used to validate, start, or reconnect a connection. `bluetoothDeviceName` is retained as display metadata only; older installs that have only the name must select their device again instead of guessing among bonded devices with duplicate names.

## The `wakelock` setting is the shared USB and Bluetooth background policy

Keep the persisted key `wakelock` and its false default. It now controls both transports: changing it while connected does not disconnect the ECU; when disabled, a true app background transition ends the session, and when enabled the foreground connection service keeps it alive. The connection-notification permission request-history flag records only whether Android's runtime prompt was already shown; permission grant state itself is always queried from Android.
