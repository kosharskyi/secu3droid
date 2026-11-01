## USB adapter presence never confirms an ECU

Only a valid firmware-info packet confirms each USB attempt. Opening plus initial firmware wait and subsequent valid-frame silence have five-second deadlines. The opening/handshake watchdog can invalidate and close an installed port while its open operation is still blocked; the read-loop silence check alone cannot provide that guarantee. The ECU streams every supported non-silent context, and the app does not select silent mode. Noise, partial frames and bad checksums cannot refresh liveness. Reset the silence timestamp after packet delivery so logger backpressure does not look like ECU power loss.

## The attachment remains admitted while unlimited USB attempts run

`UsbConnection` owns opening, handshake, reading, timeout and retry. Keep full attachment identity across failures so a device reusing the Android device ID cannot evade detach reconciliation. Initial open failure reports `Connecting`; initial firmware timeout reports `UsbAwaitingEcu`. Failure after firmware confirmation emits RETRYING; generic recovery reports `UsbReconnecting`, while ECU silence or a retry handshake timeout reports `UsbReconnectingAwaitingEcu`. Preserve an ECU-awaiting status across attempts while ignition guidance applies; repeated generic I/O failures alone must not enable that guidance. Physical detach emits STOPPED, even while retrying after an ECU timeout.

## USB permission is checked at transport admission and again before opening

The Start flow owns prompting and stale-result rejection. The transport does not request permission or retry a denied request: it checks the current platform grant before admission and before each open. A missing grant or `SecurityException` from the actual open is terminal, closes the active attempt and releases its attachment identity; ordinary I/O failures retain the attachment and continue retrying.

## An unavailable USB snapshot is not an empty attachment list

Device-list query failure preserves attachment state. A successful snapshot handles a missed detach before Start considers a device for connection. `reconcileAttachments` performs only identity cleanup; only Start makes a start request. Probe driver support before prompting and choose a serial port before opening the Android USB connection, avoiding a resource that cannot be owned by any port.
