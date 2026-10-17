## LTFT learn temperatures are signed quarter-degrees

The CLT lower/upper and IAT upper words are signed 16-bit values divided by four, including negative values. Keep their fractional Celsius values through unrelated edits, and use protocol half-away-from-zero rounding when encoding so negative half-counts do not collapse to zero.
