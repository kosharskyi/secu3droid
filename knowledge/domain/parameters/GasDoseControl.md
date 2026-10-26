## Firmware loss drops unfinished movement before the next command

The control admits every operation against `ParameterCapabilities` derived from the manager's current firmware packet. Its firmware-flow collector only invalidates accumulated manual movement when gas-dose support disappears; it does not retain a second capability flag. All control calls and firmware collection run in the owning ViewModel scope, keeping packet mutation and snapshot creation serialized on the main scope.

## Manual movement is consumed into an immutable packet snapshot

Manual presses accumulate at most -127..127. Release or periodic flush consumes the delta once; cancellation, a new receive, reset, firmware loss, or parameter-session invalidation drops it. The ECU queues packets asynchronously, so ordinary writes and manual writes must snapshot packed bytes before calling `ParameterSession.sendPacket`; the packet's temporary manual-position field must be restored immediately, and unknown packet-tail bytes must remain intact.

## UI bounds are sourced from the domain editor

`GasDoseControl` owns the numeric limits and frequency range used for both validation and the Compose editor metadata. The UI enum adds only labels, units, precision, and editor step size; it does not mutate packets or carry a second copy of the limits.
