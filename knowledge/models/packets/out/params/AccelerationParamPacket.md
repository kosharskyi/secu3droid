## Cold acceleration multiplier is unsigned fixed-point above a 100% baseline

The cold multiplier word decodes as `(raw + 128) / 128 * 100`, so raw zero means 100% and each count adds 0.78125 percentage points. Preserve the Float value and incoming word through unrelated edits; interpreting this field as signed makes words at or above `0x8000` incorrect.
