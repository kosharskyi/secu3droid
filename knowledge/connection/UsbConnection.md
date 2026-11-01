## USB adapter presence never confirms an ECU

Only a valid firmware-info packet confirms each USB attempt. Opening plus initial firmware wait and subsequent valid-frame silence have five-second deadlines. The opening/handshake watchdog can invalidate and close an installed port while its open operation is still blocked; the read-loop silence check alone cannot provide that guarantee. The ECU streams every supported non-silent context, and the app does not select silent mode. Noise, partial frames and bad checksums cannot refresh liveness. Reset the silence timestamp after packet delivery so logger backpressure does not look like ECU power loss.

## The attachment remains admitted while unlimited USB attempts run

`UsbConnection` owns opening, handshake, reading, timeout and retry. Keep full attachment identity across failures so a device reusing the Android device ID cannot evade detach reconciliation. Initial open failure reports `Connecting`; initial firmware timeout reports `UsbAwaitingEcu`. Failure after firmware confirmation emits RETRYING; generic recovery reports `UsbReconnecting`, while ECU silence or a retry handshake timeout reports `UsbReconnectingAwaitingEcu`. Preserve an ECU-awaiting status across attempts while ignition guidance applies; repeated generic I/O failures alone must not enable that guidance. Physical detach emits STOPPED, even while retrying after an ECU timeout.

## USB permission history belongs to a physical attachment

Keep at most one pending request. A denied result frees its slot but retains prompt history until detach or a successful device-list snapshot proves removal. Resume must recheck Android permission because the system attach chooser may grant access independently. Revalidate identity, support and platform permission when completing a request. Start owns the permission continuation and cancels its specific token when leaving the route; the manager does not know route state. Starting Bluetooth or disconnecting clears any pending USB permission, so a delayed result cannot start USB after another lifecycle action. A real background transition may clear pending permission, while a dialog pause alone does not stop transport work.

## An unavailable USB snapshot is not an empty attachment list

Device-list query failure preserves attachment state and pending permission. A successful snapshot handles a missed detach before considering a device for connection. `reconcileAttachments` performs only identity cleanup; `synchronizeAttachments` also considers a newly eligible device. The Activity uses cleanup on feature routes, and Start uses synchronization on entry/resume. Probe driver support and choose a serial port before opening the Android USB connection, avoiding a resource that cannot be owned by any port.
