## Carbur universal-output choices use firmware wire codes

The six numbered choices display as `1` through `6` but are transmitted as `0` through `5`; wire code `15` means disabled. ECU values outside this set remain raw until an explicit selector choice, including during unrelated edits.

## Carbur receive and unrelated edits preserve raw packet values

The inversion byte is presented as enabled when it is greater than zero, but unrelated commands preserve the original byte. Signed pressure and coolant words, selector values, and unknown packet-tail parameters are also retained; receiving never clamps values to editor bounds.

## Carbur command capability checks are field-specific and live

Fuel cutoff MAP, CTS, and selector commands require fuel injection; rev-limit commands require fuel injection or gas-dose support. Ignition selector, pressure, and the remaining controls have no firmware gate. Evaluate the current firmware when each command is sent so stale editor callbacks cannot bypass changed capabilities.
