## Minimum-pulse conversion assumes ATmega1284

The supported ECU platform is ATmega1284. Its minimum-pulse discrete is fixed at 3.2 microseconds; deprecated MCU option bits must not switch it to 4 microseconds. Minimum pulse widths store eight timer ticks per count, while maximum pulse width and rate limits use one tick per count.
