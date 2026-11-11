## Map progress counts unique addresses in each table

The ECU streams both banks without a snapshot ID or end marker. Track all 256 addresses per bank, since chunks may arrive out of order, overlap or repeat. Empty and duplicate-only chunks cannot extend the idle deadline. The UART packet period can make a healthy read last longer than a one-packet exchange, so the map deadline measures time since a new address was filled.

## The route that selected the read context restores sensors before handing it off

Parameters pauses before the LTFT destination becomes resumed. On completion or a timeout, return the ECU to sensor streaming. If the route is paused during a read, cancel it and enqueue the sensor context synchronously while that transport is still ready. A connection loss drops the partial maps and never schedules a context change from the canceled read. The next connection starts with new collectors.

## The firmware grid takes precedence over the barometric limit

Read the dedicated load grid only when FunSet selects it. Otherwise divide the configured lower-to-upper range into 15 intervals. MAP(baro) supplies the upper bound from live sensor data and rebuilds the axis when pressure changes. With no usable pressure sample, keep the map visible without load labels or a marker. Cranking suppresses the marker.

## LTFT write acknowledgements have no transaction identifier

The ECU acknowledges reset (`u 00 CA`) and EEPROM save (`u 00 09`) after the corresponding EEPROM write completes. Match both opcode and zero response data and serialize writes. A delayed acknowledgement after a timeout can be indistinguishable from a response to a later explicit write of the same kind because the protocol has no request identifier.

## A reset acknowledgement is followed by a fresh read of both maps

The acknowledgement confirms the ECU reset and EEPROM write, while displayed map values remain stale until read again. Keep those outcomes distinct in write state. If readback times out or the route is interrupted, retain the acknowledgement result and allow a manual or resumed read to verify the actual values.

## The timeout describes the current attempt

A manual or resumed read reports its own timeout even if an earlier reset timed out. Successful reads can restore the displayed maps while the reset remains unconfirmed, since reading the maps cannot establish that EEPROM was written.
