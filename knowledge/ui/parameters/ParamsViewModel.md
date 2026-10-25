## Detach the page-read registry before cancelling its jobs

The job registry is main-thread-confined, and cancelling a `viewModelScope` job may run its `finally` block immediately on the main dispatcher. Detach a snapshot and clear the registry before cancellation so those callbacks cannot mutate a live map iterator. A late job cleanup removes its entry only when that entry still refers to the same job, preserving any replacement registered during cancellation.

## Cache successful page reads only within an unchanged ECU session

Returning to a successfully read tab reuses its holder instead of resetting it to Loading and issuing another request. Any parameter write invalidates every other page conservatively, because Functions and Injection carry overlapping hidden fields; the active page remains valid because its holder contains the packet just sent. A pending read records the write revision when it actually acquires the read mutex, not when its job is created, so a page waiting behind another tab can still become cached after a later fresh read. Explicit refresh bypasses the cache, while host pause and connection loss invalidate the session cache. External ECU changes while connected require an explicit refresh or reconnect.

## Connection loss invalidates the whole parameter session in the ViewModel

Observe the connection independently of route composition: while the route is paused or recreated it may miss a disconnect. Clear pending reads, the save-confirmation job, edit callbacks and packet holders on loss, and close the send gate before clearing. Never report a packet as sent when the transport is already disconnected. Reconnection reactivates the visible page and reads fresh values.

Use the emitted connection event to invalidate the session, not a later `isConnected` lookup: that lookup may already describe a subsequent reconnect. Increment `connectionSession` on invalidation and key the pager activation effect to it. Compose may coalesce a quick disconnect/reconnect into an unchanged `Connected` value; the session key forces a fresh read without resetting the selected page or reading while the host is paused.

## The parameter exchange component owns response matching and timeout policy

The ViewModel asks `ParameterExchange` for typed packets or a typed EEPROM-save result. `ParameterPageReadGate` still controls when reads run and serializes page cleanup. Shared subscribe-before-send ordering and response deadlines belong to `EcuExchange`; `ParameterExchange` owns Functions fragments, EEPROM acknowledgment matching, and sensor-stream restoration.
