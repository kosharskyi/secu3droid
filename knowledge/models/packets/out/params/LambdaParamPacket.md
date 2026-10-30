## Lambda coolant threshold is a signed quarter-degree word

The coolant threshold word is signed 16-bit and scaled by 0.25 °C per count. Encode with protocol half-away-from-zero rounding so negative half-count edits remain symmetric with positive ones.

## Lambda edits preserve unhandled tail bytes and raw option bits

A received Lambda packet is edited in place so values outside editor ranges and unhandled tail bytes survive unrelated writes. The domain control snapshots the packed bytes before session admission; raw flag bits above the four named switches and channel-mask bits remain intact when one named bit changes.
