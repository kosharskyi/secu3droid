## Stop cancels transport work but keeps cleanup and the outbound FIFO alive

Retry and read coroutines are children of the reusable connection scope and are cancelled on stop. Transport cleanup notifications and the serial write queue use a background scope that survives stop/start; cancelling it would lose a pending `Disconnected` event and permanently kill writes for a later reconnect. Stop detaches the endpoint immediately, then closes it and publishes its state from the background scope.

## Resolve a USB driver and port before opening the USB device

Probe the selected `UsbDevice` and choose its first serial port before calling `UsbManager.openDevice`. Unsupported devices or drivers with no ports must fail before acquiring a USB connection that would otherwise need to be closed.

## The manager owns USB attachment admission and retries without a per-attachment open limit

`Secu3ConnectionManager.requestUsbConnection` resolves event payloads against the attached device, checks serial support and active-device conflicts, and keeps physical identity (device ID, system name, vendor ID and product ID) to detect replacements. A denied permission dialog is not reopened on every resume; snapshots still check current platform permission so an Android attach chooser grant can start automatically. Once permission is available, the same attached device may be opened repeatedly with cancellable delays until firmware confirms or the device detaches/background policy stops the session. Each open attempt tags both failures and raw packets, so stale work from an earlier retry cannot confirm or fail the current attempt. Home navigation is keyed to the successful attempt, while ECU data ownership remains fenced by the physical session generation.

## Attachment synchronization never treats an unavailable USB snapshot as empty

`UsbConnection.attachedDevicesSnapshot()` distinguishes an available empty list from failure to query Android's device list. `Secu3ConnectionManager.synchronizeUsbAttachments()` returns `null` without changing active-session or admission state on snapshot failure; on success it processes a missed active detach before reconciling prompted attachment history and returns only the first eligible device's permission request. The manager's `terminalSessionGeneration` stream remains the sole source for detach navigation.

The manager retains the selected session's full physical attachment identity across transient open and handshake failures; a same-device-ID replacement in a later snapshot still terminates the old session before it can be admitted.

## The manager owns one pending USB permission request

The pending permission request blocks all other USB admissions until its matching callback is consumed or the request is canceled by detach, snapshot reconciliation, stop, or backgrounding. A denied callback releases the pending slot but keeps that physical attachment in prompt history until detach or a successful snapshot shows it missing; a new snapshot still checks current platform permission so an Android chooser grant can start the device without another prompt.

## Retain the selected USB attachment identity across transient retry failures

An individual USB open or handshake failure invalidates firmware confirmation but does not end the selected attachment session. Keep its full identity alongside the session fence until matching detach, explicit stop or transport replacement; otherwise a different device reusing the same Android device ID can evade missed-detach detection during synchronization.

## Firmware information confirms each USB retry before Home or protocol access

Opening a USB serial port is not proof that an ECU is present. The manager allows protocol consumers and Home navigation only after a valid firmware-info packet for the current attempt. A bounded handshake timeout or transport error schedules another attempt rather than ending the attachment session. Detach is matched by device identity and invalidates the session generation; stale events, packets and open timeouts from earlier attempts cannot confirm a replacement attempt. Bluetooth retains its configured retry behavior.

`confirmedUsbAttemptId` is the only stored firmware confirmation; the fence tracks device/session identity only. Clear confirmation before starting a replacement or publishing retry progress, and set it before publishing `Connected`. The computed USB lock remains set if the endpoint closes before retry cleanup; only the connected/send gate additionally requires an open endpoint.

## Connection loss events must not be conflated

Parameter, sensor and diagnostics consumers invalidate their local data on each non-connected event. Keep the manager's replaying event stream rather than a `StateFlow`: a fast detach/reconnect can otherwise hide the loss and leave old ECU data active. Firmware confirmation and detach also share one coordinator lock so a parsed reply cannot reopen a session after its detach has completed.

## A request subscribes before sending and remains bound to its starting session

The shared packet stream follows transport selection changes; the per-request stream captures the selection when collected and must never accept a replacement session's reply. Both use the same parsing, generation filtering and USB-attempt confirmation path. Invoke the request callback from the raw transport's `onSubscription`, before parsing operators: a command can produce an immediate reply. With no selected transport, wait for cancellation without sending. Bluetooth firmware caching remains owned by the shared stream.

## USB opening waits on only the selected session's packet subscription

The manager has at most one selected USB session, so its pre-open readiness is a single deferred. A subscription callback may complete it only while its captured selection is still current. Cancellation clears the pending signal, while the waiter clears it after completion only if the field still refers to that same deferred. This prevents a late collector or waiter from releasing or erasing a replacement session.

## One background preference controls both transports

The persisted `wakelock` key and default are retained, but the preference applies to both USB and Bluetooth. Toggling it off while connected stops only the foreground service; the live transport and logger continue until the app actually stops or the ECU disconnects. If background work is off, the manager ends either transport and logging on a real Activity stop and publishes that ended session's generation; configuration recreation does not count as leaving. With the preference on, the service stays owned through connection retries. `ConnectionServiceController` owns Android service calls, while a service owner token remains distinct from the connection generation so late destruction from an old service cannot stop a replacement session.

## Unexpected destruction of the current connection service follows app visibility

The manager validates the destroyed service owner token and session generation before acting. If the app remains visible, it preserves either transport and restores the service only when background work is enabled. If the app is backgrounded, it stops transport and logging and publishes the ended session's generation so navigation returns to StartScreen. Confirmed USB does not bypass this service lifecycle policy; returning to the unlocked app admits the still-attached device again.

## Connection notification permission is requested only from Settings

Android 13+ notification access is requested only after the user enables background work or selects the explicit connection-notifications row. A denied request does not change the background preference or stop a live connection. The permission grant is queried from Android whenever the Settings route resumes; once granted, the current foreground service notification is refreshed in place through `ConnectionNotificationPublisher`, without restarting the ECU transport. The channel ID `Background connection chanel` is stable because Android stores user channel choices against it.

## USB permission dialogs do not mask true background transitions

Only Activity stop or lock handling marks the app backgrounded; a permission dialog's pause alone does not. A true background transition cancels the manager's pending USB permission token, so a late platform result cannot open a connection behind the app and a later foreground snapshot can admit the now-authorized attached device.

## Bluetooth retry events belong to one manager generation

`BtConnection` tags every state event with the generation supplied by `Secu3ConnectionManager`. The manager ignores callbacks from replaced Bluetooth attempts, even when the replacement also uses Bluetooth. Transient failures keep the foreground service; only the transport's explicit retry-exhaustion event publishes the ended generation and stops service/logging. The service itself does not infer terminal state from the connection event stream.

## Terminal session results survive Activity recreation until a new attempt

The manager exposes the latest ended session generation as a replaying nullable value. A new USB or Bluetooth attempt, or a manual stop, clears that marker; a manual stop does not publish a terminal generation. Background and service stops publish the ended generation after cleanup. UI queues Start navigation against the generation and cancels only that queued terminal navigation when the manager clears it; the connection-state event stream remains non-conflated for data consumers.
