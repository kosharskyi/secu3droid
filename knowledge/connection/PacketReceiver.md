## Frame boundaries are independent of transport read boundaries

Bluetooth and USB must feed the same receiver logic. Keep one receiver per transport read loop, retain a partial frame between reads, and create a fresh receiver after reconnect. Only a literal `@` starts a frame; after overflow or a malformed frame discard noise until the next header. An escaped `@` is payload and must not restart assembly.

## The 250-byte limit applies after decoding escapes

The header and descriptor are literal, but each subsequent byte, including either checksum byte, can occupy two wire bytes. A decoded 250-byte packet may therefore require up to 498 bytes before the terminating CR. Limiting the encoded buffer to 250 rejects otherwise valid packets.

## Transmit escaping includes both checksum bytes and excludes the terminator

Call `escapeTx` after appending checksum bytes and append the literal terminating CR afterwards. Skipping the final input byte leaves the last checksum unescaped; checksum values `0x0D`, `0x0A` or `0x21` can then truncate or restart a packet at the ECU.

## Receive and transmit escape alphabets differ

Incoming `0x0A 0x82` represents `@`; outgoing `0x0A 0x81` represents `!`. Both directions use `0x83` for CR and `0x84` for the escape byte. Reject incomplete or unknown escape pairs as a whole frame: silently removing them can leave a different byte sequence with an apparently valid checksum.

## Fletcher checksum excludes the header but includes payload length

Checksum input omits the two literal header/descriptor bytes and the two checksum bytes. After processing the decoded payload, update the first sum with its byte count and update the second sum once more. Wire order is second sum followed by first sum. Compute before escaping and validate after unescaping; including transport escapes or the CR would break compatibility with the ECU.

## A reader returning EOF must enter the connection error path

`readNext` throws `EOFException` on `-1`, including after a partial frame. Continuing the Bluetooth loop on EOF can spin forever while the socket still reports connected. Checksum rejection and malformed frames remain recoverable within the current reader; EOF requires reconnecting.
