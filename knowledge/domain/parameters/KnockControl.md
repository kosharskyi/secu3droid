## Knock selectors retain raw wire codes independently of localized labels

The firmware accepts BPF frequency codes `0..63` and integration timeout codes `0..31`. The domain validates explicit selections against those protocol bounds, while the screen maps these codes to the ordered resource arrays. Incoming out-of-range codes remain raw and unchanged until that selector is explicitly changed.

## Knock enable and sensor-channel mask are separate wire meanings

Any positive `useKnockChannel` byte presents as enabled, but unrelated edits preserve the exact byte; an explicit toggle writes `1` or `0`. Each `selectedChanels` bit selects the first or second knock sensor for a cylinder, with bits `0..7` corresponding to cylinders `1..8`; it is not a cylinder enable mask.

## Knock edits validate only the selected field and keep other packet data raw

Each explicit numeric command checks finiteness and that field's editor bounds. It does not snap to editor step sizes or impose an angle relationship. Receive is passive, and unrelated commands retain signed/fractional values, unknown selector codes, other field bytes, and unknown packet tail. A rejected session admission restores the same packet instance and leaves the published values unchanged.
