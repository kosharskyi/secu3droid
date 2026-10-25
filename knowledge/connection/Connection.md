## Stop cancels transport work but keeps cleanup and the outbound FIFO alive

Retry and read coroutines are children of the reusable connection scope and are cancelled on stop. Transport cleanup notifications and the serial write queue use a background scope that survives stop/start; cancelling it would lose a pending `Disconnected` event and permanently kill writes for a later reconnect. Stop detaches the endpoint immediately, then closes it and publishes its state from the background scope.

## Resolve a USB driver and port before opening the USB device

Probe the selected `UsbDevice` and choose its first serial port before calling `UsbManager.openDevice`. Unsupported devices or drivers with no ports must fail before acquiring a USB connection that would otherwise need to be closed.

## The connection manager owns USB attachment admission

`Secu3ConnectionManager.requestUsbConnection` is the only app entry point for opening USB. It resolves event payloads against the currently attached device, checks serial support and active-device conflicts, and owns per-attachment attempt tokens keyed by device ID, system device name, vendor ID and product ID. This identity check matters when Android reuses a device ID after replacement. A denied permission result consumes the pending prompt slot but retains that attachment's attempt token, so a later Android attach chooser grant can open it once without repeating the prompt. Permission completion rechecks attachment identity, driver support and platform permission; detach or resume reconciliation against full device identities invalidates stale tokens.

## Attachment synchronization never treats an unavailable USB snapshot as empty

`UsbConnection.attachedDevicesSnapshot()` distinguishes an available empty list from failure to query Android's device list. `Secu3ConnectionManager.synchronizeUsbAttachments()` leaves active-session and admission state unchanged on snapshot failure; on success it processes a missed active detach before reconciling pending attachment history and admitting at most the first eligible device. Its typed result carries both USB session termination and any permission/open request so consumers do not infer USB ownership from the shared connection state.

The manager retains the selected session's full physical attachment identity across `ConnectionFailed` and `ConnectionLost`; a same-device-ID replacement in a later snapshot still terminates the old session before it can be admitted.

## Retain the selected USB attachment identity after a connection failure

`ConnectionFailed` and `ConnectionLost` invalidate firmware confirmation but retain the selected USB attachment. Keep its full identity alongside the session fence until matching detach, explicit stop or transport replacement; otherwise a different device reusing the same Android device ID can evade missed-detach detection during synchronization.

## Firmware information confirms a USB session

USB open is one attempt per physical attachment. The manager gates protocol consumers and Home navigation until a valid firmware-info packet confirms the ECU; a bounded handshake timeout or transport error ends that attempt. Detach is matched by device ID and invalidates the active generation, so old packet work and unrelated detach broadcasts cannot affect a replacement session. Bluetooth retains its configured retry behavior.

## Connection loss events must not be conflated

Parameter, sensor and diagnostics consumers invalidate their local data on each non-connected event. Keep the manager's replaying event stream rather than a `StateFlow`: a fast detach/reconnect can otherwise hide the loss and leave old ECU data active. Firmware confirmation and detach also share one coordinator lock so a parsed reply cannot reopen a session after its detach has completed.

## USB terminal state distinguishes an unconfirmed failure from a lost session

The manager checks firmware confirmation before clearing the USB session fence. It publishes `ConnectionFailed` when opening or the initial handshake fails, and `ConnectionLost` when transport failure ends an already confirmed session. UI can choose the matching message from the event itself without retaining session history across Activity recreation. A physical detach still publishes `Disconnected` and does not create a connection-failure notification.

## Foreground service ownership follows connection generations

`Secu3ConnectionManager` owns the service policy and `ConnectionServiceController` owns Android start/stop calls. A service owner token is distinct from a connection generation: a late `onDestroy` may clear or stop only the service and transport generation it still owns. Starting service ownership for a replacement connection invalidates callbacks from an older instance. Confirmed USB requires the service regardless of the background preference; Bluetooth requires the preference. USB detach leaves the running service to observe `Disconnected` through its existing 20-second debounce. Explicit user stops clean up transport, logger and service together in the manager.
