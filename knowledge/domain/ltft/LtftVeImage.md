## Manager VE maps use 12-bit packed cells in wire order

The ATmega1284 Manager flash image contains four 16×16 VE maps at offsets 117955, 120788, 123621, and 126454; its EEPROM image contains one at offset 907. Each adjacent pair occupies three bytes, low cell first. LTFT corrections are applied to the same 256-cell row-major order. Updating a VE map requires refreshing the enclosing image CRC while preserving every other byte.

## Accept only finalized ATmega1284 Manager images

The last four bytes identify the image as `1284`. Flash images also need Manager-compatible firmware metadata, both fuel-injection and ATmega1284 config flags, and a valid firmware CRC. EEPROM images require valid parameter and table CRCs. Do not treat a firmware build artifact with a placeholder CRC as a Manager-saved image.

## HEX and A90 wrap the same finalized flash bytes

Manager reads Intel HEX record types `00` through `03`, interprets `02` as a segment address shifted left by four bits, and fills omitted flash bytes with `FF`. Validate record checksums and EOF before applying the same binary image checks. Manager writes 16-byte uppercase records with CRLF and switches to segment `1000` at byte `10000` (hex). EEPROM uses the raw BIN layout.
