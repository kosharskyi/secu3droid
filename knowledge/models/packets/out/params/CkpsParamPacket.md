## Hall window width and BTDC angle are signed thirty-seconds of a degree

Both Hall angle words are signed 16-bit values divided by 32. Preserve negative fractional angles and use protocol half-away-from-zero rounding when packing, especially for negative half-counts.

## Mutable CKPS commands are snapshotted before asynchronous send

`snapshot()` captures the packed bytes and returns a fresh copy from `pack()`. Keep this separate from normal packet packing so the connection queue never observes later edits to the control's mutable receive packet.
