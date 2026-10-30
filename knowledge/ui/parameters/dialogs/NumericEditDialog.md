## Float display text does not replace the untouched request value

The initial editor text is formatted to the request precision, but confirming without edits returns the original `FloatValue.value`. Display rounding must not quantize parameter or diagnostic values; only a changed, validated text value is parsed for confirmation.

## Numeric stepping keeps its fallback in the editor session

The dialog's last valid fallback remains saveable under the edit session ID. Partial text and `NaN` use that fallback when stepping, while integer input is parsed as `BigInteger` so large typed values are stepped before clamping without overflow.

## Float formatting is stable across device locales

Editor text uses `Locale.US` decimal formatting. Precision values from zero through four are honored; any other precision uses four decimal places so diagnostics and parameter editors produce the same input representation.
