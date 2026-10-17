## Hall window width and BTDC angle are signed thirty-seconds of a degree

Both Hall angle words are signed 16-bit values divided by 32. Preserve negative fractional angles and use protocol half-away-from-zero rounding when packing, especially for negative half-counts.
