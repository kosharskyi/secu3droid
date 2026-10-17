## S3L sensor flags have fixed desktop-compatible padding positions

The 252-byte little-endian S3L record stores airflow at byte 32, one padding byte at 33, the main and additional flag words at 34 and 36, then two padding bytes at 38–39 before TPS at 40. These positions are part of the external desktop logger ABI; the padding values are ignored. Android writes `0xCC` as a deterministic filler, while desktop logs may contain arbitrary values there.
