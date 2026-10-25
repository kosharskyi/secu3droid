## Read revisions begin when a page owns the serialized ECU request slot

A page job may wait behind cancellation cleanup from another page. Capture the write revision only after its read acquires `ParameterPageReadGate`'s mutex; otherwise a write during the wait would incorrectly mark a fresh response stale.

## Disconnect events invalidate; transport state only gates admission

Use emitted connection events to end the current page session and reject late results. Check the manager's live connected state again before admitting a send or starting a request, since its event collector can lag a physical disconnect. Do not use that later state lookup to replace event ordering across a fast reconnect.

## Pause cancels work without clearing editable UI state

Pause advances the session token, cancels reads and pending save confirmation, and clears successful-page cache markers. A later completion from the old work must not publish into the resumed route; the visible page will request fresh data when reactivated.

## Read-job bookkeeping is confined to the caller's main scope

The session's methods and event callbacks run on the ViewModel's main scope. Detach the read-job registry before cancellation because a job's `finally` block may run immediately and remove its entry. Late cleanup must remove an entry only if it still belongs to that job, preserving a replacement read.
