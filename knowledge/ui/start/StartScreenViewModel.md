## Bluetooth configuration must resolve to a bonded address

The persisted MAC address is authoritative. A legacy stored name cannot validate a selection, and an address that is malformed or no longer bonded must send the user through device selection again. Names may change or collide, so they are display metadata only.

## Bluetooth preference work belongs to the cancellable connection job

Saving a selected bonded device and validating the stored bonded address happen inside the same `connectionJob` that awaits manager preflight. Canceling the UI action must cancel that entire continuation so a late preference read cannot start a new connection after the user presses Cancel.

## Connection progress follows manager events and current preparation

The connecting indicator combines the active preference/preflight job, pending Bluetooth pairing, and the manager's current running-but-not-connected state. Manager state events refresh it; do not poll connection or firmware properties. A canceled preparation job may only clear its own reference, so an older completion cannot erase a replacement job's progress.

## Resuming an open Bluetooth picker retains its rows

Returning from pause resumes discovery only while the Bluetooth picker remains visible and idle. Resume must retain the current device list and order; only an explicit user-started scan reseeds it from bonded devices. The route can also return from location settings after its callback already started discovery, so the resume path must be idempotent.

Bluetooth permission requirements are owned by `BluetoothPermissionPolicy`. Connection requests and discovery resume intentionally use different legacy permission sets, and the API 31 Xiaomi/POCO connection exception must not be applied to discovery.

## StartScreen presents USB state but does not own connection admission

USB attachment can happen on any route or while the Activity starts from Android's attach intent, so StartScreen only presents device state and forwards user selection. `Secu3ConnectionManager` admits the device, de-duplicates permission prompts, and retries open/firmware failures indefinitely while the Activity is visible. Permission completion rechecks attachment identity and Android's current grant. USB retries do not use the Bluetooth retry preference, and no manual action or cable replug is needed after a transient failure or return from background.
