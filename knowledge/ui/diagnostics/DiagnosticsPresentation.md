## A missing input packet is different from a zero-valued packet

Diagnostics inputs receive a nullable packet while firmware data is unavailable. Keep that null state visible as unavailable; do not substitute a default `DiagInputPacket`, because its zeroes and false bits are legitimate readings once a packet exists. Digital input state uses a compact colored text badge and matching accessibility state description. These are read-only ECU indicators, not switch-like controls.

## Output switches remain user-command controls

The diagnostic output checkboxes are intentional ECU commands, not passive status indicators. Preserve their firmware/permission gates and invoke the existing callback only from user interaction; the compact grouped presentation must not turn them into read-only colored indicators.

## Channel choices commit only on explicit selection

The channel selector shares the parameter selection row and choice dialog. Option IDs remain the indices of the firmware-specific channel list; Cancel and dismissal send no command. Dialog visibility is keyed to the firmware family so losing or switching firmware data closes the old choices without sending a selection.

## BL, DE, and TACH output state is separate from its direction enable

For these special outputs, the lower bit is state and the next higher bit is direction: BL uses 11/12, DE 13/14, and TACH 23/24. Direction=0/state=0 is Hi-Z, direction=1/state=0 drives LOW, and direction=1/state=1 drives HIGH. The checkbox reflects the state bit, while the additional option controls direction. Enabling resets the output LOW and sends it; disabling clears both bits to Hi-Z. Ignore stale output callbacks while direction is disabled.

## Reapplying unchanged additional-output options must preserve driven state

The packet direction flags are the source of truth for whether BL/DE and TACH options changed. An unchanged option confirmation sends no packet and must preserve HIGH outputs; changing one group resets only that group, leaving the other group's LOW/HIGH/Hi-Z bits untouched.

## Initialize ignition outputs from the diagnostic family before sending

The first DIAGINP `flags` byte identifies SECU-3T when nonzero. Set IGN_OUT1/2 to HIGH for SECU-3T and LOW for SECU-3i before sending any diagnostic output packet. These family-specific defaults match the desktop manager’s protective initialization for legacy SECU-2 Lite ignition transistor/coil hardware. If firmware family is not known yet, suppress output sends until the first DIAGINP supplies it. Send the initialized packet only once; later input packets must not replace output bits the user has changed.

## Keep the displayed initial duty equal to the packet duty

Diagnostics starts at 50% duty in both the controls and the desktop manager. The percentage is serialized as a byte, so 50% becomes 128 after rounding; keep the UI value derived from the packet so the first channel command sends what the control displays.

## The generator-selected output cannot be manually toggled

Channel zero is NONE; each nonzero channel index maps through its firmware family's full ordered output list. Use that mapping both to disable the matching output row/checkbox and to reject stale manual callbacks in the controller. Keep TACH_O in the SECU-3i mapping even when its row is hidden by the direction-enable option, because filtering the display must not shift protocol channel indices. Selecting a different channel or NONE unlocks the old output while preserving the existing channel-selection send.
