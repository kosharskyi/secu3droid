## Live and EEPROM reads are independent

The active `v` stream starts as soon as the route is resumed, regardless of the auto-read preference or the EEPROM `x` response. The saved-errors preference is read asynchronously, and a stored `false` suppresses only the automatic EEPROM read. The preference controls the next route session, so toggling it while this screen is open does not unexpectedly send a read command.

## Live mode and its display hold last for the route ViewModel

Pausing the route or reconnecting clears packet masks, drafts, timers, and write state, while preserving the selected live mode and short-error hold setting for the next active session. Disabling live mode cancels its packet collector, clears its mask, and selects the normal sensors mode. Firmware-info recovery selects `v` only when live mode remains enabled.

## Editing saved errors preserves unknown ECU bits

Only the known 26 bits can be changed through row controls. Select-all and deselect-all affect those bits and retain any other set bits from the EEPROM mask. A write requires a fresh saved mask, an actual draft change, a matching opcode-2/data-0 acknowledgement, and a subsequent `x` response.

## Uncertain writes require a fresh read before another write

After acknowledgement or readback timeout the UI must not resend automatically. The write status remains uncertain if a manual refresh also times out. A successful fresh EEPROM response clears that status and establishes a new editing baseline. Clear-all is a separate confirmed write of a zero mask through the same acknowledgement/readback transaction.

The protocol acknowledgement has no request ID. A very late acknowledgement from a previous user-initiated write cannot be perfectly distinguished from the acknowledgement for a later write, even after the user refreshes and starts a new transaction.

## Brief live errors expire from their last active packet

With the hold option enabled, each set bit remains visible for 500 ms from its most recent `v` packet. Expiration uses monotonic time and is scheduled even when no later packet arrives.
