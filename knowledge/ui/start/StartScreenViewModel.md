## Bluetooth configuration must resolve to a bonded address

The persisted MAC address is authoritative. A legacy stored name cannot validate a selection, and an address that is malformed or no longer bonded must send the user through device selection again. Names may change or collide, so they are display metadata only.

## Resuming an open Bluetooth picker retains its rows

Returning from pause resumes discovery only while the Bluetooth picker remains visible and idle. Resume must retain the current device list and order; only an explicit user-started scan reseeds it from bonded devices. The route can also return from location settings after its callback already started discovery, so the resume path must be idempotent.
