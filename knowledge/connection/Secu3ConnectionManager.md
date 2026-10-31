## Transport request identity and logical ECU session identity serve different lifetimes

A selected transport request remains stable throughout its internal retries. Each explicit RETRYING outcome ends the manager's current logical session and allocates a new one; STOPPED ends that logical session and clears selection. This makes timeout followed by cable detach two distinct terminal results without resetting UI navigation deduplication. Stale transport generations or event sequences cannot affect a replacement selection.

## Loss side effects are common to both transports

Consume the typed outcome rather than inferring loss from packet silence or reconstructing retry history. Ended sessions stop logging and publish the terminal generation for UI navigation. A continuing session rebinds the existing foreground-service owner to the new logical generation; a stopped transport releases that service. A fresh Connected clears the old terminal marker before publishing connection status.

## Logger packet writes backpressure the common collector

Await each `SecuLogger.logPacket` call. UI sampling must not change logger delivery; fire-and-forget logging or another unlimited queue would remove this backpressure. The manager forwards parsed transport packets without caching their latest values for screens.

## Firmware state describes only the selected transport

The nullable canonical `firmwareInfo` is updated from the owning event before its connection-state notification. Clear it when selection ends or the transport invalidates its protocol context. `fwInfo` reads that same canonical state. Keep loss events non-conflated so rapid recovery cannot hide data invalidation from parameter, diagnostics or sensor consumers.

## One observed background preference controls both transports

Unknown policy stays unknown until DataStore succeeds; a later storage failure returns it to unknown. A real background transition ends either transport only when background work is disabled; permission-dialog pause and configuration recreation do not count. Preference writes wait until the committed policy is observed. Disabling background work while visible stops only the service until the app actually leaves.

## Service destruction is fenced by owner token and logical session

A late destroy from an older owner cannot stop a replacement. Destruction of the current owner preserves a visible connection and restores the service when enabled; in background it stops transport and logging and publishes terminal navigation. Logical retry rollover updates the same owner's connection generation without restarting Android's service.

## Terminal navigation persists until recovery or a replacement request

The terminal flow replays the latest ended logical generation across Activity recreation. Fresh connection admission, successful recovery or manual stop clears it. UI retains a queued Start until it can navigate, while a successful USB Home claim cancels an obsolete queued Start. The manager knows no routes or screens.
