## Signed condition thresholds use signed wire words and protocol rounding

Thresholds are signed 16-bit words for condition IDs 0 (CTS, ×4), 11 (AANG, ×32), 14 (ATS, ×4), 28 (GRTS, ×4), 30 (TMP2, ×4), 34 (TPSDOT, ×1), and 39/40 (FTS/OTS, ×4). Encoding these conditions must round negative half-counts away from zero; unsigned condition encoders retain their existing behavior.

## Failed edits restore the captured raw Universal Outputs packet fields

The Universal Outputs control checkpoints each output's complete flags word, two condition IDs, and all four raw threshold words, plus the paired logic byte. Restore that checkpoint in place when validation, packing, or session admission fails; decoded floats lose unknown-condition words and VSPD's 999.9 display clamp, and replacing the packet would discard its unhandled tail.
