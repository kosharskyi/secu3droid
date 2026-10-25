## Bluetooth configuration must resolve to a bonded address

The persisted MAC address is authoritative. A legacy stored name cannot validate a selection, and an address that is malformed or no longer bonded must send the user through device selection again. Names may change or collide, so they are display metadata only.

## Resuming an open Bluetooth picker retains its rows

Returning from pause resumes discovery only while the Bluetooth picker remains visible and idle. Resume must retain the current device list and order; only an explicit user-started scan reseeds it from bonded devices. The route can also return from location settings after its callback already started discovery, so the resume path must be idempotent.

## StartScreen presents USB state but does not own connection admission

USB attachment can happen on any route or while the Activity starts from Android's attach intent, so StartScreen only presents device state and forwards user selection. `Secu3ConnectionManager` admits the device, de-duplicates permission prompts, and retries open/firmware failures indefinitely while the Activity is visible. Permission completion rechecks attachment identity and Android's current grant. USB retries do not use the Bluetooth retry preference, and no manual action or cable replug is needed after a transient failure or return from background.
