## Fuel and oil temperatures use signed quarter-degree words

The fuel and oil temperature samples are signed 16-bit values scaled at 0.25 °C per count. High-bit words encode negative temperatures.

## Exhaust temperature is signed and remains unclamped

The exhaust gas temperature sample is also a signed 16-bit count scaled at 0.25 °C per count. Preserve negative readings and values above 999 °C; this channel has no coolant-style clamp.

## Signed sensor words must retain negative physical values

Fuel tank level, oil pressure and MAF use signed 16-bit wire counts; negative raw values represent negative physical readings and must not be decoded as large positive values. Knock retard is signed too, except raw `0x7FFF`, which disables the reading and leaves it unused on a fresh packet.
