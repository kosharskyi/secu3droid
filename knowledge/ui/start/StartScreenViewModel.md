## Bluetooth configuration must resolve to a bonded address

The persisted MAC address is authoritative. A legacy stored name cannot validate a selection, and an address that is malformed or no longer bonded must send the user through device selection again. Names may change or collide, so they are display metadata only.

## Bluetooth preference work belongs to the cancellable connection job

Saving a selected bonded device and validating the stored bonded address happen inside the same `connectionJob` that awaits manager preflight. Canceling the UI action must cancel that entire continuation so a late preference read cannot start a new connection after the user presses Cancel.

## Connection progress follows manager events and current preparation

The connecting indicator combines the active preference/preflight job, pending Bluetooth pairing, and the manager's current running-but-not-connected state. Manager state events refresh it; do not poll connection or firmware properties. A canceled preparation job may only clear its own reference, so an older completion cannot erase a replacement job's progress.

## USB ignition guidance follows the transport's liveness state

The Start route keeps the USB hint generic while permission/open/firmware setup is in progress. After USB reports `UsbAwaitingEcu` through the manager following a packet or handshake timeout, replace the hint with the localized ignition prompt. Keep it visible through automatic retries while the USB device remains attached; a valid firmware response or detach clears it. Do not infer ECU power from the platform USB attachment or permission result.

## Resuming an open Bluetooth picker retains its rows

Returning from pause resumes discovery only while the Bluetooth picker remains visible and idle. Resume must retain the current device list and order; only an explicit user-started scan reseeds it from bonded devices. The route can also return from location settings after its callback already started discovery, so the resume path must be idempotent.

Bluetooth permission requirements are owned by `BluetoothPermissionPolicy`. Connection requests and discovery resume intentionally use different legacy permission sets, and the API 31 Xiaomi/POCO connection exception must not be applied to discovery.

## StartScreen presents USB state but does not own connection admission

USB attachment can happen on any route or while the Activity starts from Android's attach intent, so StartScreen only presents device state and forwards user selection. `UsbConnection` owns device admission, permission de-duplication and unlimited open/firmware retries; the manager forwards actions and applies the shared foreground/background policy. Permission completion rechecks attachment identity and Android's current grant. USB retries do not use the Bluetooth retry preference, and no manual action or cable replug is needed after a transient failure or return from background.

## Connected navigation belongs to the Activity ViewModel

The shared status collector handles both Bluetooth and USB Home transitions. StartRoute must not react to a replayed Connected independently, because two navigation owners can consume the same recovery while a permission dialog pauses the Activity.
