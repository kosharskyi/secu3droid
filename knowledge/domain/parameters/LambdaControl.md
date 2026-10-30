## MS edits truncate to whole tens while unrelated fields retain raw values

The MS editor accepts 0–2500 and stores `(value / 10) * 10`; for example 129 becomes 120, matching desktop integer division. A parsed 2550 remains untouched through unrelated edits even though it exceeds the editor maximum. Invalid explicit values are rejected rather than clamped.

## Lambda command refusal restores the original packet in place

`ParameterSession` admission may reject after the mutable packet has been edited. Restore every public packet field before returning false, leaving the received object identity and its protected unhandled tail intact. Send a copied packed byte snapshot so later edits cannot change a queued write.

## Raw Lambda codes and option bits stay visible and preserved

Sensor codes outside NBO 0 and WBO 1 remain in state as their raw integer until an explicit valid selection. Editing one of the four named flag bits or any channel bit must preserve all other flag and channel bits. Unknown firmware is permissive for base fields; channel and STOICH commands still require their stricter known-firmware capabilities, and MS rechecks that strokes-per-step remains zero.
