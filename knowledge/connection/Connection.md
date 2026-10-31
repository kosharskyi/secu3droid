## Stop cancels transport work but keeps cleanup and the outbound FIFO alive

`Connection` owns the connection job and attempt IDs for both transports. Its reusable I/O scope is canceled on stop; preference observation, captured-endpoint cleanup and queued writes use a separate scope that survives stop/start. A stopped endpoint is detached immediately, before asynchronous close, so an old cleanup can never close its replacement.

## Physical endpoint availability and ECU readiness are different facts

`isConnected` checks only the physical endpoint. Production consumers use `isReady`; USB sets readiness after valid firmware information, while Bluetooth sets it after socket establishment. `readyAttemptId` is derived from the active attempt and Connected state. Invalidate the attempt before failed-attempt cleanup so a late firmware reply cannot restore readiness; there is no separate stored readiness flag. Manual-disconnect admission is a transport property; forced background/service cleanup bypasses that user-action guard.

## Packet parsing belongs to the receiving attempt

Both readers pass checksum-valid frames through `receivePacket`. It parses with the current fuel-flow calibration and protocol firmware context, then rechecks ownership before publishing. Sensor frames are skipped until settings load; firmware parsing must still proceed. A later preference read failure retains the last calibration. The private protocol firmware context is cleared with the attempt. Only the manager exposes observable firmware state, scoped to the current transport request.

## Requests subscribe before sending and cannot cross reconnects

The shared packet flow follows current attempts without replay. The request overload captures its starting attempt, subscribes, then invokes the send callback. It must reject replacement-attempt replies even if the same transport and device remain selected. Firmware bootstrap happens inside each transport before it reports Connected; it does not depend on a manager or screen packet subscriber.

## Event ownership and ordering must survive asynchronous delivery

`ConnectionEvent.generation` identifies the manager request that owns the transport job; `sequence` orders outcomes within that transport. Initial manager admission and synchronous detach may apply an outcome before the event collector catches up, so sequence checks reject already-applied or older outcomes. During an ongoing job, consume the event stream in order: applying a newer Connected snapshot on resume could skip a queued loss outcome. A normal state has no `end`; `RETRYING` ends an established logical session while transport work continues, and `STOPPED` releases that transport.
