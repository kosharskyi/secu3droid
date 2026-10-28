## ZERO modes are mutually exclusive and OCTANE is locked during raw zero mode

The desktop clears `zero_adv_ang_oct` when enabling ZERO and clears the raw `zero_adv_ang` byte when enabling ZERO_WITH_OCTANE. Disabling either mode changes only that mode. Firmware tests the raw zero byte first, so a stale OCTANE edit must be rejected by the control while `zero_adv_ang` is nonzero, including raw values above one.

## Rejected edits restore the original packet in place

Angles packets may carry unhandled extension bytes and unknown ignition flag bits. Snapshot the packed command before sending and restore all known fields and the full flag byte on admission refusal; replacing the packet would discard its extension bytes.

## Incoming numeric values stay raw until their own explicit edit

Publish all six decoded signed 1/32-degree values without editor clamping or snapping. Validate only the newly submitted value against the desktop editor bounds; unrelated commands must retain incoming fractions and out-of-range wire values.
