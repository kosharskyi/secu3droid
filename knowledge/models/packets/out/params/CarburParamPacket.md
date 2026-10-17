## Fuel-cut coolant threshold is a signed quarter-degree word

The coolant threshold word is signed 16-bit and scaled by 0.25 °C per count. Encode with protocol half-away-from-zero rounding so negative half-count edits remain symmetric with positive ones.

## Fuel-enrichment threshold is a signed MAP word

`feOnThresholds` is a signed 16-bit MAP value with 1/64-unit resolution. Decode negative and boundary words as signed values and use protocol half-away-from-zero rounding when packing, matching desktop behavior for negative half-counts. The current UI range remains nonnegative; signed decoding preserves the actual protocol representation without broadening that editor range.
