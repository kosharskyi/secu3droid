## Live and saved errors remain visible independently

The live and EEPROM masks can arrive separately or fail independently. A zero mask is valid data, so the list can show an empty result for the received data while the other source is loading or has timed out.

Keep the empty result as the last list item so it can center in the viewport while the read controls keep their positions. In short windows it follows the controls in the scroll order.

## Catalog positions match ECU protocol bits

Every localized error-title array and the shared blink-code array follow bit order 0–25. The BC column is a label from SECU-3 Manager, and its value is independent of the bit index. Keep the firmware-start entry at bit 15 with BC 00.

An ECU can return set bits outside this catalog. The list displays a separate message for these bits, so an unknown-only mask cannot appear as an empty result.

## A draft does not change the displayed EEPROM state

During editing, the saved badge still reflects the last ECU read. The checkbox reflects the draft. A saved-only error remains visible after unchecking it so the removal can be undone before writing. Leaving the route or losing the connection dismisses both write confirmation actions, so a confirmation cannot follow a draft from a previous session.
