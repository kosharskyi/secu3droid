## Five IDLREG correction fields are signed fixed-point values

`iFac1` and `iFac2` use signed 1/256 counts, the minimum/maximum regulator angles use signed 1/32-degree counts, and turn-on temperature uses signed quarter-degree counts. Preserve negative fractional values and use protocol half-away-from-zero rounding when encoding these words.
