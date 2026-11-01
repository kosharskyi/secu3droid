## Transport selection does not identify ECU sessions

`currentConnection` remains selected through automatic retries and is cleared on STOPPED. Outcomes remain in the transport's queue until the manager drains them under its connection-selection lock; the manager has no generation counters or event-owner checks. Select a newly started transport before releasing that lock and consume every queued outcome rather than substituting a latest-state snapshot.

USB start returns whether a run was admitted, not whether it is still running. An admitted run can queue `STOPPED` before the manager selects it; checking `isRunning` instead would discard that run's terminal outcome. Select the admitted run and then drain its events.

## Loss side effects are common, but visible retry states remain transport specific

Consume the typed outcome rather than inferring loss from packet silence or reconstructing retry history. Established losses stop logging and continuing USB work emits `Disconnected` before retry progress. Bluetooth established retries publish `Reconnecting` without `Disconnected`, so feature routes remain in place. Initial USB handshake failures do not emit `Disconnected`.

## New transport commands are admitted only after the selected transport ends

`startUsbConnection` and `startBtConnection` do not replace an active or retrying `currentConnection`. Before rejecting a command, drain that connection's queued terminal outcomes so a pending `STOPPED` can release the slot. USB attachment reconciliation remains cleanup-only and may end a detached or physically replaced session before a subsequent Start command admits a replacement. The manager never requests USB permission; Start checks the current grant and the transport checks again before opening. Existing transport retries remain owned by their connection job. USB recovery is expressed by `UsbReconnecting` or `UsbReconnectingAwaitingEcu`; consumers must not reconstruct it from an initial handshake status or a transport-history flag.

Before a USB command, reconcile missing/replaced attachments and drain the old terminal outcome. Android can reuse a device ID, and both physical devices use the same `UsbConnection` instance. A failed attachment snapshot must remain non-destructive. Start cancels its pending permission request when Bluetooth starts, so a delayed grant cannot start USB later.

## Logger packet writes backpressure the common collector

Await each `SecuLogger.logPacket` call. UI sampling must not change logger delivery; fire-and-forget logging or another unlimited queue would remove this backpressure. The manager forwards parsed transport packets without caching their latest values for screens.

## The merged packet stream relies on exclusive transport operation and attempt fencing

`receivedPacketFlow` merges both transport streams without consulting manager selection state. Only one transport request is admitted at a time; each `Connection` rejects packets outside its active attempt. A one-shot request captures the current `Connection` when collection begins and remains bound to that attempt instead of following a later `currentConnection` change.

## Firmware state describes only the current transport request

The nullable canonical `firmwareInfo` is updated from the owning event before its connection-state notification. Clear it when `currentConnection` ends or the transport invalidates its protocol context. `fwInfo` reads that same canonical state. Keep loss events non-conflated so rapid recovery cannot hide data invalidation from parameter, diagnostics or sensor consumers.

## One observed background preference controls both transports

Unknown policy stays unknown until DataStore succeeds; a later storage failure returns it to unknown. A real background transition ends either transport only when background work is disabled; permission-dialog pause and configuration recreation do not count. Preference writes wait until the committed policy is observed. Disabling background work while visible stops only the service until the app actually leaves.

## The foreground service follows statuses and never controls the transport

Request the service only when background work is enabled and the published status is `Connected`, `Reconnecting`, `UsbReconnecting` or `UsbReconnectingAwaitingEcu`. Initial `Connecting` and `UsbAwaitingEcu` do not request it. Every `Disconnected` stops it; a following USB retry status can start it again. A later confirmed connection starts it when enabled.

The injected `ServiceManager` only issues Android service commands. It has no connection policy, owner tokens or ECU identity. Service destruction performs local foreground cleanup without calling the connection manager. A service-start failure must leave transport/status intact and allow a later reconciliation to retry; it must not terminate the outcome collector. Unknown preference policy preserves the current request only while the status remains eligible, never after a disconnect.

The manager does not cache whether Android currently runs the service. Reissue start or stop commands whenever status and the known preference call for that action; Android treats repeated service commands idempotently. For an eligible status with an unknown preference, issue no policy command. Any ineligible status always issues stop, including while preference state is unknown. Notification refresh uses the same eligible-status and enabled-preference check.

## Session endings and retry progress must remain distinct state emissions

For USB, emit `Disconnected` after loss cleanup and before retry progress. `UsbReconnecting` and `UsbReconnectingAwaitingEcu` describe recovery after confirmed connectivity, while `UsbAwaitingEcu` describes the initial handshake timeout. A later physical detach emits another `Disconnected` after retry progress, even when firmware never recovered. Bluetooth established retries skip `Disconnected` and end in `Disconnected` only when stopped or exhausted. Preserve stream ordering rather than exposing a parallel terminal-generation channel to UI consumers.
