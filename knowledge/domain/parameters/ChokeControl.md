## Choke command admission follows both page support and field capabilities

Every Choke command rechecks the current firmware snapshot. Loss of Choke support rejects stale page callbacks and clears pending manual movement; the carburetor-only regulator fields additionally require carburetor firmware. Null firmware remains permissive for editor previews, matching the shared capability contract.

## Choke manual snapshots preserve packet data while keeping the wire delta transient

Manual movement is accumulated outside the received packet and consumed into an immutable packet snapshot. Ordinary edits send a zero-delta snapshot. The packet helper temporarily writes the signed delta for packing and restores it immediately, preserving unknown flag bits and trailing packet data for asynchronous sends.

## Manual movement helper instances must stay isolated

`ManualPositionDelta` is injected unscoped, so each `ChokeControl` and `GasDoseControl` owns independent pending movement. Do not scope the helper to the ViewModel or singleton: the two parameter pages share a ViewModel and otherwise a flush or cancellation could consume movement from the other control.
