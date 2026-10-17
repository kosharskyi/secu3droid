## Knock control threshold is a signed quarter-degree word

The coolant threshold wire word is signed 16-bit and scaled by 0.25 °C per count. High-bit raw values represent negative temperatures; treating them as unsigned can turn a cold threshold into a very large positive value. Encode with protocol half-away-from-zero rounding so negative half-count edits retain their sign.

## Knock window begin and end angles are signed thirty-seconds of a degree

Both window angle words are signed 16-bit values divided by 32. Their encoders use protocol half-away-from-zero rounding, including for negative half-count edits.

## Retard step, advance step, and maximum retard are signed thirty-seconds of a degree

These three correction words are signed 16-bit values divided by 32. Preserve negative fractional angles and encode with protocol half-away-from-zero rounding so negative half-count edits do not collapse toward zero.
