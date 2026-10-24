## Signed condition thresholds use signed wire words and protocol rounding

Thresholds are signed 16-bit words for condition IDs 0 (CTS, ×4), 11 (AANG, ×32), 14 (ATS, ×4), 28 (GRTS, ×4), 30 (TMP2, ×4), 34 (TPSDOT, ×1), and 39/40 (FTS/OTS, ×4). Encoding these conditions must round negative half-counts away from zero; unsigned condition encoders retain their existing behavior.
