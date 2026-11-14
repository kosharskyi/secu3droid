## Read revisions begin when a page owns the serialized ECU request slot

A page job may wait behind cancellation cleanup from another page. Capture the write revision only after its read acquires `ParameterPageReadGate`'s mutex; otherwise a write during the wait would incorrectly mark a fresh response stale.

## Disconnect events invalidate; transport state only gates admission

Use emitted connection events to end the current page session and reject late results. Check the manager's live connected state again before admitting a send or starting a request, since its event collector can lag a physical disconnect. Do not use that later state lookup to replace event ordering across a fast reconnect.

## Reconnect restarts the selected page without a UI session key

Keep the selected page across connection loss, cancel its work and clear its cache, then reload it on the next `Connected` event. Compose can conflate a rapid loss/recovery pair into one displayed `Connected`, so restarting must not depend on recomposition or an exported session counter. Pause and route closure clear the selection so recovery cannot restart work for an inactive route. `Invalidated` carries no session identity; cancellation tokens remain private to the domain component.

## Pause cancels work without clearing editable UI state

Pause advances the session token, cancels reads and pending save confirmation, and clears successful-page cache markers. A later completion from the old work must not publish into the resumed route; the visible page will request fresh data when reactivated.

## Read-job bookkeeping is confined to the caller's main scope

The session's methods and event callbacks run on the ViewModel's main scope. Detach the read-job registry before cancellation because a job's `finally` block may run immediately and remove its entry. Late cleanup must remove an entry only if it still belongs to that job, preserving a replacement read.

## A TPS measurement must not resume on a later page activation

TPS uses the page-read mutex but does not use the retry-on-activation flow or populate the Functions cache. Each reply supplies only the raw voltage. The Functions deactivation and session invalidation events cancel the caller's measurement job and discard its draft, including when a reply is pending or the preview is already visible.

## DBW confirmation is attempted after a page has loaded

The probe shares the page-read mutex because a DBW query changes the stream mode. Page reads, TPS measurement and EEPROM save cancel it before using the stream. A canceled probe can resume once ordinary work completes. A completed probe is kept through host pause and is cleared on disconnect or route closure. A timeout means support is unconfirmed because the firmware silently ignores an unsupported request and the transport can also lose a reply.
