## Enter diagnostics only after the received-packet subscription is active

`DiagnosticsSession.start` observes the manager's non-conflated connection event flow and collects the selected transport's received packets. Its subscription callback marks the transport available and queues ENTER before parsing can deliver an immediate ACK. ACK 6 queues at most one DIAGINP request for that connection; reconnect resets that gate and permits one new entry.

## Leave and restore sensor streaming only for an entered live session

Closing before entry or after the transport is no longer connected sends no cleanup tasks. Closing an entered live session queues LEAVE followed by sensor restore, in that order. The route graph owns one ViewModel and therefore one session across the additional-options dialog.

## A fresh diagnostic input is required after every connection loss

Any non-connected event clears the diagnostic input and disables controls, resets the selected channel and actuator word locally, and retains frequency and duty. Keep the last known ECU family only to reapply SECU-3T's safe ignition HIGH mask across repeated loss events. On initial entry, cached firmware information selects the safe output mask before input arrives; after connection loss, the next fresh input reinitializes the family from its flags and sends the initialized output packet once. Later inputs must not overwrite user-controlled outputs.

## Channel zero is NONE and family channel IDs include hidden outputs

`DiagnosticOutput.secu3TChannels` and `secu3IChannels` omit NONE; the protocol index is the list index plus one, with zero reserved for NONE. The SECU-3i catalog always includes TACH_O before GPA6_O, even while UI hides the TACH_O row. Channel validation uses the full catalog, and the state exposes the domain-resolved generator output so UI does not reproduce index mapping.

## Generator values are validated by the session before packet mutation

Frequency must be finite and within 10–5000 Hz; duty must be finite and within 0–100%. Channel selection accepts only NONE through the final family channel. Invalid values are ignored without changing state or sending a packet. Re-selecting a valid channel and reapplying a valid generator value still queues the command, matching the existing command behavior.

## BL, DE, and TACH output bits require their direction bits

BL uses state/direction bits 11/12, DE uses 13/14, and TACH_O uses 23/24. Enabling a direction resets that output LOW and queues its packet; disabling clears state and direction to Hi-Z. Manual changes are ignored while direction is disabled and generator-selected outputs are rejected. Applying additional options changes BL/DE first and TACH second, sending one snapshot per changed group; unchanged options preserve driven HIGH state.

The session changes BL and DE direction together, and publishes `enableBlDe` only when both are enabled. Both the command guard and UI use `DiagnosticsState.canSetOutput`, while the command guard also checks the live transport so a stale state cannot send after disconnection.
