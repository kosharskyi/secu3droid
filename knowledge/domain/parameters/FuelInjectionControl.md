## Timing and pulses-per-liter values stay physical until the editor opens

Fuel Injection values expose timing/cranking timing as signed fractional degrees and pulses-per-liter as its decoded fixed-point `Float`. UI selector/editor mapping rounds only the presented whole-number values; unrelated packet writes must keep the original fractional wire values.

## Firmware and map guards apply at command admission

`FuelInjectionControl` reads the connection manager's current firmware for each command. Fuel injection support gates all edits, SECU-3T blocks additional corrections, differential pressure, and injector row switching, and each bank's map flag blocks edits to that bank's main timing while leaving cranking timing editable.

## Configuration updates set a valid pulse count first

Use `InjctrParPacket.validConfigurations()` and `validPulses(config)` for choices and validation. When changing a bank configuration, retain its current pulse count if the new configuration permits it; otherwise assign the first valid count before updating the configuration so derived constants are recalculated from a valid pair.
