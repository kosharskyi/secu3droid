## Functions edits keep the received packet as the source of truth

`ParameterExchange` emits one `FunctionParameters` value only after receiving every function-name fragment and the FUNSET settings. `FunctionsControl` then owns the original mutable packet so unknown selector codes, unrelated flag bits, read-only telemetry, calibration fractions, and trailing bytes survive edits. Each admitted command is an immutable packed snapshot; a rejected send restores the mutable packet.

## Universal output choices carry explicit protocol codes

`MAPSEL_PETROL`, `MAPSEL_GAS`, and `GAS_V_UNI` use codes `0..5` and disabled sentinel `15`. Incoming raw code `6` remains preserved until an explicit choice; the user-facing `no` label is never used to infer a packet value. Editing either MAPSEL nibble preserves its sibling nibble.

## MAF confirmation preserves the desktop Float calculation

When MAF is selected on firmware without fuel injection, the existing editor presents total engine displacement. Confirmation updates `injCylDisp` and `mafload_const` in one command, retaining the desktop's Float intermediate before the wire truncates the constant to an integer. The command is refused if source, live firmware, cylinder count, or displacement changed out of bounds while the editor was open.
