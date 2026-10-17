## Minimum-pulse conversion assumes ATmega1284

The supported ECU platform is ATmega1284. Its minimum-pulse discrete is fixed at 3.2 microseconds; deprecated MCU option bits must not switch it to 4 microseconds. Minimum pulse widths store eight timer ticks per count, while maximum pulse width and rate limits use one tick per count.

## All four injector timing words are signed sixteenths of a degree

Main and cranking timing for both banks decode as signed 16-bit words divided by 16. Preserve these fractional values in the packet when another field changes; the UI rounds only for its whole-degree editor, and writes use the protocol's half-away-from-zero rounding rule.

## Injector pulses-per-liter stays a physical Float

`fffConst` is an unsigned 16-bit fixed-point physical value (`word * 60000 / 65536`). Keep the decoded Float so unrelated parameter writes can reproduce its original word. `SensorsPacket` has a separate fixed app-side conversion constant; it is not this ECU parameter.
