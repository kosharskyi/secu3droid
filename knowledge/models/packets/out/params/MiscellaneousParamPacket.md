## VSS wire distance keeps its reciprocal Float precision

The ECU stores the reciprocal `32768000 / pulsesPerKm` as a 16-bit word. Retain the parsed physical value as a Float so unrelated packet edits re-emit the original word; round only when preparing the integer UI state or explicit integer editor value.

## Hop angles retain one-thirty-second degree wire precision

The start angle is a signed 16-bit value and duration is unsigned; both decode as `raw / 32` degrees. Keep their fractional Float values in the packet so integer UI display and unrelated edits do not quantize them; use half-away-from-zero rounding when encoding the signed start angle, including negative half-counts.

## Ignition-cutoff threshold is signed on the wire

The ignition cutoff threshold word is signed 16-bit. It remains an integer field with its existing nonnegative UI range; signed parsing preserves negative and boundary wire values without changing editor bounds.

## EVAP airflow start rounds to 32-RxL wire counts

The outgoing start value is scaled by 1/32 and rounded half away from zero. Preserve that rounding for manually entered values not divisible by 32; truncating integer division changes the encoded airflow start.
