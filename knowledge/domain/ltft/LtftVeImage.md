## Manager `.bin` VE maps use 12-bit packed cells in wire order

The ATmega1284 Manager flash image contains four 16×16 VE maps at offsets 117955, 120788, 123621, and 126454; its EEPROM image contains one at offset 907. Each adjacent pair occupies three bytes, low cell first. LTFT corrections are applied to the same 256-cell row-major order. Updating a VE map requires refreshing the enclosing image CRC while preserving every other byte.

## Accept only finalized ATmega1284 Manager images

The last four bytes identify the image as `1284`. Flash images also need Manager-compatible firmware metadata, both fuel-injection and ATmega1284 config flags, and a valid firmware CRC. EEPROM images require valid parameter and table CRCs. Do not treat a firmware build artifact with a placeholder CRC as a Manager-saved image.
