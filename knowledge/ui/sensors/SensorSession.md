## Sensor packets are valid only within one connected transport session

`SensorsViewModel` owns one connection-scoped packet collector. It clears the sensor, raw ADC, and firmware snapshot whenever the connection leaves `Connected` and again when a new connection starts. The transports use non-replaying shared packet flows, so route collection must not invent a cached initial packet or keep the previous session's snapshot. Firmware already cached by the connection manager may seed only the first session when the ViewModel is created while connected; later sessions wait for that session's firmware packet. Keep packet availability separate from the configured Room card lists: configuration remains observable while offline, while gauge values and indicator state stay unavailable until a fresh `SensorsPacket` arrives.

## Gauge and indicator configuration is independent from ECU packet cadence

The ordered Room flows drive the card lists directly. Add, delete, and reorder operations must update those flows without waiting for another ECU packet; packet arrival only refreshes each configured card's displayed reading.
