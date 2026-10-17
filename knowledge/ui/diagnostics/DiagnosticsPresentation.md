## A missing input packet is different from a zero-valued packet

Diagnostics inputs receive a nullable packet while firmware data is unavailable. Keep that null state visible as unavailable; do not substitute a default `DiagInputPacket`, because its zeroes and false bits are legitimate readings once a packet exists. Digital input state uses a compact colored text badge and matching accessibility state description. These are read-only ECU indicators, not switch-like controls.

## Output switches remain user-command controls

The diagnostic output checkboxes are intentional ECU commands, not passive status indicators. Preserve their firmware/permission gates and invoke the existing callback only from user interaction; the compact grouped presentation must not turn them into read-only colored indicators.

## Channel choices commit only on explicit selection

The channel selector shares the parameter selection row and choice dialog. Option IDs remain the indices of the firmware-specific channel list; Cancel and dismissal send no command. Dialog visibility is keyed to the firmware family so losing or switching firmware data closes the old choices without sending a selection.

## BL, DE, and TACH output state is separate from its direction enable

For these special outputs, the lower bit is state and the next higher bit is direction: BL uses 11/12, DE 13/14, and TACH 23/24. Direction=0/state=0 is Hi-Z, direction=1/state=0 drives LOW, and direction=1/state=1 drives HIGH. The checkbox reflects the state bit, while the additional option controls direction. Enabling resets the output LOW and sends it; disabling clears both bits to Hi-Z. Ignore stale output callbacks while direction is disabled.
