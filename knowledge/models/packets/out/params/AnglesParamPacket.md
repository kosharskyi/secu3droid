## All six ignition angle fields encode signed thirty-seconds with protocol rounding

Maximum/minimum angle, correction, decrease/increase speed, and shift ignition time use signed 16-bit words at 1/32 degree per count. Preserve negative values and use protocol half-away-from-zero rounding when packing, particularly for negative half-counts.
