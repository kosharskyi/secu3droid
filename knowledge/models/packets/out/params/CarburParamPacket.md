## Fuel-cut coolant threshold is a signed quarter-degree word

The coolant threshold word is signed 16-bit and scaled by 0.25 °C per count. Encode with protocol half-away-from-zero rounding so negative half-count edits remain symmetric with positive ones.
