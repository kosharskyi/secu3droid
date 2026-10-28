## After-start inputs retain whole strokes between four-stroke wire counts

The desktop integer editor accepts every whole stroke from 0 through 1020, even though its spin control advances by four. Keep the entered integer in the packet and published values; `StarterParamPacket.pack()` performs the established half-away-from-zero conversion to a four-stroke count.

## Starter edits preserve packet extensions and incoming values

Receive publishes the original field values without applying editor bounds. An explicit edit validates only its own value and current firmware capability. If session admission refuses it, restore known packet fields in place; copying the data class would lose unhandled extension bytes.

## Prime pulse commands use the fixed ATmega1284 discrete

Prime pulse conversion remains owned by `StarterParamPacket` and uses its fixed 3.2-microsecond discrete. Do not derive a scale from firmware flags.
