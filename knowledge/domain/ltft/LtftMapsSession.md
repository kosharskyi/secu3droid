## Map progress counts unique addresses in each table

The ECU streams both banks without a snapshot ID or end marker. Track all 256 addresses per bank, since chunks may arrive out of order, overlap or repeat. Empty and duplicate-only chunks cannot extend the idle deadline. The UART packet period can make a healthy read last longer than a one-packet exchange, so the map deadline measures time since a new address was filled.

## The route that selected the read context restores sensors before handing it off

Parameters pauses before the LTFT destination becomes resumed. On completion or a timeout, return the ECU to sensor streaming. If the route is paused during a read, cancel it and enqueue the sensor context synchronously while that transport is still ready. A connection loss drops the partial maps and never schedules a context change from the canceled read. The next connection starts with new collectors.

## The firmware grid takes precedence over the barometric limit

Read the dedicated load grid only when FunSet selects it. Otherwise divide the configured lower-to-upper range into 15 intervals. MAP(baro) supplies the upper bound from live sensor data and rebuilds the axis when pressure changes. With no usable pressure sample, keep the map visible without load labels or a marker. Cranking suppresses the marker.
