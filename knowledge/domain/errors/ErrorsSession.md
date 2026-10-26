## Saved and active masks are independent and arrive in sequence

An empty screen is valid only after both the saved EEPROM response (`u,0,2`) and active `v` mask arrive, even when either mask is zero. Request saved errors before selecting active mode. Connection setup may deliver firmware information after the first active request, so the session reselects active mode when that packet arrives.

## Error reads are fenced by route and connection generations

The session observes the manager's non-conflated connection event stream and reads only while the route is resumed and transport is connected. Losing either condition cancels the worker and clears the snapshot. Each replacement session advances a generation so a response or timeout from canceled work cannot update current state; the packet request subscription is established before sending its command.

## Clear writes require a matching acknowledgement and a fresh readback

Only an opcode-2, data-0 `u` acknowledgement advances the saved-error clear transaction to an EEPROM readback. Keep a single clear in flight because the protocol has no request IDs. A delayed acknowledgement that crosses a user-initiated retry cannot be perfectly correlated with the earlier or later clear. A nonzero readback is still successful because active faults may be recorded again. Timeouts report uncertainty and never retry the write; active-error collection resumes after either timeout.

## Active timeout leaves the collector available for late packets

Only the first active-error packet is timed. After timeout, keep listening so a late `v` packet or firmware-info recovery can complete the screen. Explicit read retry cancels that collector and begins the full saved-then-active sequence while preserving the prior clear outcome.

## Snapshot capabilities rely on clearing stale packets when the session stops

`ErrorsSnapshot` does not store connection or route-resume flags. Its computed clear and retry capabilities are valid because pause and every non-connected event cancel the worker and replace the snapshot with an empty one. Retaining masks across either boundary would expose actions for data from an earlier ECU session.
