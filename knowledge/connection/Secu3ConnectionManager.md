## The current transport request remains stable through its retries

`currentConnection` and its request generation identify the admitted transport from connection start through internal retries. Each explicit RETRYING outcome ends the current logical ECU session and allocates a new session generation while keeping that transport request. STOPPED clears `currentConnection`. These identities fence transport events and foreground-service ownership; UI consumers receive connection states without session IDs. Stale transport generations or event sequences cannot affect a replacement request.

## Loss side effects are common to both transports

Consume the typed outcome rather than inferring loss from packet silence or reconstructing retry history. Ended sessions stop logging and emit `Disconnected` through the common connection-state stream. A continuing session rebinds the existing foreground-service owner to the new logical generation; a stopped transport releases that service. A continuing USB retry may then publish `UsbAwaitingEcu` or `InProgress`; the preceding `Disconnected` must remain observable. Initial USB handshake failures do not emit it because no ECU session has ended.

## Logger packet writes backpressure the common collector

Await each `SecuLogger.logPacket` call. UI sampling must not change logger delivery; fire-and-forget logging or another unlimited queue would remove this backpressure. The manager forwards parsed transport packets without caching their latest values for screens.

## The merged packet stream relies on exclusive transport operation and attempt fencing

`receivedPacketFlow` merges both transport streams without consulting manager selection state. Only one transport request is admitted at a time; each `Connection` rejects packets outside its active attempt. A one-shot request captures the current `Connection` when collection begins and remains bound to that attempt instead of following a later `currentConnection` change.

## Firmware state describes only the current transport request

The nullable canonical `firmwareInfo` is updated from the owning event before its connection-state notification. Clear it when `currentConnection` ends or the transport invalidates its protocol context. `fwInfo` reads that same canonical state. Keep loss events non-conflated so rapid recovery cannot hide data invalidation from parameter, diagnostics or sensor consumers.

## One observed background preference controls both transports

Unknown policy stays unknown until DataStore succeeds; a later storage failure returns it to unknown. A real background transition ends either transport only when background work is disabled; permission-dialog pause and configuration recreation do not count. Preference writes wait until the committed policy is observed. Disabling background work while visible stops only the service until the app actually leaves.

## Service destruction is fenced by owner token and logical session

A late destroy from an older owner cannot stop a replacement. Destruction of the current owner preserves a visible connection and restores the service when enabled; in background it stops transport and logging and publishes `Disconnected`. Logical retry rollover updates the same owner's connection generation without restarting Android's service.

## Session endings and retry progress must remain distinct state emissions

Emit `Disconnected` after loss cleanup and before publishing the transport's retry/terminal status. `UsbAwaitingEcu` by itself can describe an initial handshake timeout, so it cannot identify an established-session loss. A later physical detach emits another `Disconnected` after retry progress, even when firmware never recovered. Preserve stream ordering rather than exposing a parallel terminal-generation channel to UI consumers.
