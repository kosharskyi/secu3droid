## TPS gradients use twice the TPS physical multiplier

Both directions use `128 * (64 * 2) * 0.0025`; using `64 * 64` on write amplifies the TPS gradient by 32 even when another Functions setting was edited. Incoming FUNSET adds a read-only TPS ADC word after the MAF constant, so an outgoing payload is not directly an incoming test fixture.

## Signed half-counts follow the manager's rounding rule

Manager MathHelpers::Round rounds a negative half-count away from zero; Kotlin roundToInt rounds it toward positive infinity. Use protocolRoundToInt for the corrected TPS, ADC and negative LTFT encoders so values on that boundary do not silently become zero.

## APPS1 raw is the final known read-only word

FUNSET includes an unsigned 16-bit APPS1 ADC voltage after the APPS1 gradient. It is read-only telemetry used for learning and must not be resent; only bytes after this word are unknown extensions to preserve.

## TPS raw is signed while APPS1 raw is unsigned

The TPS ADC learning word after the MAF constant is a signed 16-bit voltage count; APPS1 raw at the end is an unsigned 16-bit count. Preserve this difference when decoding high-bit samples.
