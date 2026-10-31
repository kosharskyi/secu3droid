## USB adapter presence never confirms an ECU

Only a valid firmware-info packet confirms each USB attempt. Opening plus initial firmware wait and subsequent valid-frame silence have five-second deadlines. The opening/handshake watchdog can invalidate and close an installed port while its open operation is still blocked; the read-loop silence check alone cannot provide that guarantee. The ECU streams every supported non-silent context, and the app does not select silent mode. Noise, partial frames and bad checksums cannot refresh liveness. Reset the silence timestamp after packet delivery so logger backpressure does not look like ECU power loss.

## The attachment remains admitted while unlimited USB attempts run

`UsbConnection` owns opening, handshake, reading, timeout and retry. Keep full attachment identity across failures so a device reusing the Android device ID cannot evade detach reconciliation. An initial open or handshake failure has no ended logical session. Failure after firmware confirmation emits RETRYING; the manager handles the common ended-session consequences. Physical detach emits STOPPED, even while retrying after an ECU timeout.

## USB permission history belongs to a physical attachment

Keep at most one pending request. A denied result frees its slot but retains prompt history until detach or a successful device-list snapshot proves removal. Resume must recheck Android permission because the system attach chooser may grant access independently. Revalidate identity, support and platform permission when completing a request. The manager guards foreground admission and cancels pending permission on real background transitions; a dialog pause alone does not stop transport work.

## An unavailable USB snapshot is not an empty attachment list

Device-list query failure preserves admission and pending permission. A successful snapshot handles a missed detach before admitting an eligible replacement. Probe driver support and choose a serial port before opening the Android USB connection, avoiding a resource that cannot be owned by any port.
