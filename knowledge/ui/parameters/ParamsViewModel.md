## Detach the page-read registry before cancelling its jobs

The job registry is main-thread-confined, and cancelling a `viewModelScope` job may run its `finally` block immediately on the main dispatcher. Detach a snapshot and clear the registry before cancellation so those callbacks cannot mutate a live map iterator. A late job cleanup removes its entry only when that entry still refers to the same job, preserving any replacement registered during cancellation.

## Cache successful page reads only within an unchanged ECU session

Returning to a successfully read tab reuses its holder instead of resetting it to Loading and issuing another request. Any parameter write invalidates every other page conservatively, because Functions and Injection carry overlapping hidden fields; the active page remains valid because its holder contains the packet just sent. A pending read records the write revision when it actually acquires the read mutex, not when its job is created, so a page waiting behind another tab can still become cached after a later fresh read. Explicit refresh bypasses the cache, while host pause and connection loss invalidate the session cache. External ECU changes while connected require an explicit refresh or reconnect.

## Connection loss invalidates the whole parameter session in the ViewModel

Observe the connection independently of route composition: while the route is paused or recreated it may miss a disconnect. Clear pending reads, the save-confirmation job, edit callbacks and packet holders on loss, and close the send gate before clearing. Never report a packet as sent when the transport is already disconnected. Reconnection reactivates the visible page and reads fresh values.

Use the emitted connection event to invalidate the session, not a later `isConnected` lookup: that lookup may already describe a subsequent reconnect. Increment `connectionSession` on invalidation and key the pager activation effect to it. Compose may coalesce a quick disconnect/reconnect into an unchanged `Connected` value; the session key forces a fresh read without resetting the selected page or reading while the host is paused.

## Parameter requests subscribe before sending and have a bounded response wait

`readParameterPacket` waits at most 10 seconds for a response after activation, including each stage of Functions' two-stage read. Its cleanup restores sensor streaming before the next page acquires the read mutex. A timeout becomes a retryable page error; lifecycle cancellation remains cancellation. EEPROM save acknowledgements use the same subscription-before-send ordering, because an immediate reply can otherwise arrive before the collector. Only one save acknowledgement is pending at a time; cancelling the session discards that waiter rather than accepting an acknowledgement from a later connection.

## Request timeout must escape transformLatest as an ordinary error

`TimeoutCancellationException` thrown inside the page gate's `transformLatest` child only cancels that child; the outer collector can keep waiting forever. Convert an expired request deadline to `TimeoutException` after checking that the parent coroutine is still active. Preserve parent cancellation so switching tabs, leaving the screen and losing the connection never display a timeout error.
