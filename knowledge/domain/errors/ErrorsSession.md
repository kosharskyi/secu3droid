## Live and EEPROM reads are independent

The active `v` stream starts whenever the route is resumed and the ECU is connected, regardless of the auto-read preference or the EEPROM `x` response. The saved-errors preference is read asynchronously, and a stored `false` suppresses only the automatic EEPROM read. The preference controls the next route session, so toggling it while this screen is open does not unexpectedly send a read command.

## The display hold setting lasts for the route ViewModel

Pausing the route or reconnecting clears packet masks, drafts, timers, and write state, while preserving the short-error hold setting for the next active session. Firmware-info recovery reselects `v` while the route is resumed and connected.

## Individual removals only affect known bits already saved by the ECU

A draft can remove and restore known bits from the latest EEPROM read. Live errors cannot add bits to the draft, including during the short-error hold. Unknown saved bits can be removed only by clearing the entire history. A write requires a fresh saved mask, an actual draft change for individual removals, a matching opcode-2/data-0 acknowledgement, and a subsequent `x` response.

## Uncertain writes require a fresh read before another write

After acknowledgement or readback timeout the UI must not resend automatically. The write status remains uncertain if a manual refresh also times out. A successful fresh EEPROM response clears that status and establishes a new editing baseline. Clear-all is a separate confirmed write of a zero mask through the same acknowledgement/readback transaction.

The protocol acknowledgement has no request ID. A very late acknowledgement from a previous user-initiated write cannot be perfectly distinguished from the acknowledgement for a later write, even after the user refreshes and starts a new transaction.

## Brief live errors expire from their last active packet

With the hold option enabled, each set bit remains visible for 500 ms from its most recent `v` packet. Expiration uses monotonic time and is scheduled even when no later packet arrives.
