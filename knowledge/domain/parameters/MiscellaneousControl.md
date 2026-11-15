## Rollback must restore the received packet in place

`MiscellaneousParamPacket.copy()` does not copy the inherited unknown-parameter tail. Keep a scalar backup only for rollback, restore those fields on the original packet, and capture outgoing bytes with `snapshot()` before handing a command to the asynchronous session.

## EVAP edits validate typed and encoded starts together

The editor's entered begin value can differ from the 32-RxL wire start. The slope search validates both decoded end values against the domain limits and minimum delta; retain the bounded `-8..8` raw-slope search and its nearest-safe-candidate choice.

Use the larger of the entered and rounded wire starts as the slope reference. Otherwise an entered pair near the upper limit can decode 16 RxL above 2,000,000 after the start rounds upward.

## Changing an EVAP threshold can also move its neighbor

Manager moves the other endpoint when a change would leave less than 16,500 RxL between the endpoints. Each editor's bounds reserve room for that separation independently of the loaded pair. Normalize both endpoints before choosing a slope, then emit one packet for the complete edit. Existing 32-RxL start rounding can still make the displayed start differ from the decoded wire value.

## Miscellaneous display values retain packet precision

Hop angles and VSS stay raw Floats in `MiscellaneousValues`. Round only when presenting or opening an integer editor, so unrelated writes preserve one-thirty-second-degree and reciprocal VSS wire values.
