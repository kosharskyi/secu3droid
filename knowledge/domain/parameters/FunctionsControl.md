## Functions edits keep the received packet as the source of truth

`ParameterExchange` emits one `FunctionParameters` value only after receiving every function-name fragment and the FUNSET settings. `FunctionsControl` then owns the original mutable packet so unknown selector codes, unrelated flag bits, read-only telemetry, calibration fractions, and trailing bytes survive edits. Each admitted command is an immutable packed snapshot; a rejected send restores the mutable packet.

## Universal output choices carry explicit protocol codes

`MAPSEL_PETROL`, `MAPSEL_GAS`, and `GAS_V_UNI` use codes `0..5` and disabled sentinel `15`. Incoming raw code `6` remains preserved until an explicit choice; the user-facing `no` label is never used to infer a packet value. Editing either MAPSEL nibble preserves its sibling nibble.

## MAF confirmation preserves the desktop Float calculation

When MAF is selected on firmware without fuel injection, the existing editor presents total engine displacement. Confirmation updates `injCylDisp` and `mafload_const` in one command, retaining the desktop's Float intermediate before the wire truncates the constant to an integer. The command is refused if source, live firmware, cylinder count, or displacement changed out of bounds while the editor was open.

## Pressure curve calculations commit offset and gradient together

SECU-3 Manager allows the calculator to produce values beyond the narrower limits of the manual offset and slope editors. The calculator checks that both coefficients fit signed FUNSET words and that the rounded slope is nonzero. Apply rechecks current firmware support and submits both fields in the same FUNSET snapshot.

## Compare calibration voltages in volts

SECU-3 Manager compares the entered numbers before converting point units. With mixed V and mV, this can give the wrong order. Android requires the second point's voltage to be higher after conversion. Conversion factors follow the Manager implementation, including its constants for mmHg, technical atmospheres and psi.

## TPS and APPS1 measurements leave the editable FUNSET snapshot intact

After each position is confirmed, wait for the Manager's one-second settling interval and read only the selected sensor's voltage from a fresh FUNSET. The rest of the measurement reply is not loaded into the editor. The signed voltage difference gives the gradient's polarity, and a span slightly above Manager's 0.1 V threshold can still overflow a signed gradient word. Reuse the selected sensor's packet scale. APPS1 requires confirmed DBW both during capture and when applying the result.
