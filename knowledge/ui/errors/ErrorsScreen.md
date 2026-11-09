## An unavailable mask has no badge or count

The live and EEPROM masks can arrive separately or fail independently. A zero mask is valid data. Show the available badges and counts while the other source is loading or has timed out.

## Catalog positions match ECU protocol bits

Every localized error-title array and the shared blink-code array follow bit order 0–25. The BC column is a label from SECU-3 Manager, and its value is independent of the bit index. Keep the firmware-start entry at bit 15 with BC 00.

An ECU can return set bits outside this catalog. The totals include them and the list displays a separate message, so an unknown-only mask cannot appear as an empty result.

## A draft does not change the displayed EEPROM state

During editing, the saved badge still reflects the last ECU read. The checkbox reflects the draft. Leaving the route or losing the connection dismisses both write confirmation actions, so a confirmation cannot follow a draft from a previous session.
