## Stop cancels transport work but keeps cleanup and the outbound FIFO alive

`Connection` owns the connection job and attempt IDs for both transports. Its reusable I/O scope is canceled on stop; preference observation, captured-endpoint cleanup and queued writes use a separate scope that survives stop/start. A stopped endpoint is detached immediately, before asynchronous close, so an old cleanup can never close its replacement.

## Physical endpoint availability and ECU readiness are different facts

`isConnected` checks only the physical endpoint. Production consumers use `isReady`; USB sets readiness after valid firmware information, while Bluetooth sets it after socket establishment. `readyAttemptId` is derived from the active attempt and Connected state. Invalidate the attempt before failed-attempt cleanup so a late firmware reply cannot restore readiness; there is no separate stored readiness flag. The manager's disconnect command always stops the selected transport. User-facing controls enforce any session lock before calling the manager; background cleanup uses the same stop path.

## Packet parsing belongs to the receiving attempt

Both readers pass checksum-valid frames through `receivePacket`. It parses with the current fuel-flow calibration and protocol firmware context, then rechecks ownership before publishing. Sensor frames are skipped until settings load; firmware parsing must still proceed. A later preference read failure retains the last calibration. The private protocol firmware context is cleared with the attempt. Only the manager exposes observable firmware state, scoped to the current transport request.

## Requests subscribe before sending and cannot cross reconnects

The shared packet flow follows current attempts without replay. The request overload captures its starting attempt, subscribes, then invokes the send callback. It must reject replacement-attempt replies even if the same transport and device remain selected. Firmware bootstrap happens inside each transport before it reports Connected; it does not depend on a manager or screen packet subscriber.

## Pending statuses stay inside the transport until serialized consumption

The transport queues full outcomes under `connectionLock` and only signals that work is available. The manager is the single queue consumer and drains it while holding its connection-selection lock. Wake signals may conflate; the actual outcomes must not, because even an immediate loss/recovery pair invalidates in-flight consumer work. No job identity or generation is attached to an outcome.

Starting a new run clears undelivered old outcomes before installing its job. The manager must consume the old disconnect before admitting that run; a queued wake from the old run then finds only the current queue. Source coroutines still check `connectionJob` before enqueueing, and asynchronous endpoint closing never publishes another status.

The drain callback runs with the connection lock held, so its caller must already hold the manager lock. Worker publication only queues and signals; it must not call the manager directly and invert that lock order. A normal outcome has no `end`; `RETRYING` marks established loss while transport work continues, and `STOPPED` releases that transport.
