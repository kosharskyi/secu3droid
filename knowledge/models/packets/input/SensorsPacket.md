## Fuel and oil temperatures use signed quarter-degree words

The fuel and oil temperature samples are signed 16-bit values scaled at 0.25 °C per count. High-bit words therefore encode negative temperatures, unlike unsigned sensor channels such as voltage or flow.

## Exhaust temperature is signed and remains unclamped

The exhaust gas temperature sample is also a signed 16-bit count scaled by 4 °C per count. Preserve negative readings and values above 999 °C; this channel has no coolant-style clamp.
