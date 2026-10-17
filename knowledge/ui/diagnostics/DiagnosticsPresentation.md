## A missing input packet is different from a zero-valued packet

Diagnostics inputs receive a nullable packet while firmware data is unavailable. Keep that null state visible as unavailable; do not substitute a default `DiagInputPacket`, because its zeroes and false bits are legitimate readings once a packet exists. Digital input state uses a compact colored text badge and matching accessibility state description. These are read-only ECU indicators, not switch-like controls.

## Output switches remain user-command controls

The diagnostic output checkboxes are intentional ECU commands, not passive status indicators. Preserve their firmware/permission gates and invoke the existing callback only from user interaction; the compact grouped presentation must not turn them into read-only colored indicators.
