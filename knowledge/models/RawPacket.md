## Raw packet parsing receives complete frames after transport checksum validation

The transport owns frame checksum validation. `RawPacket` validates the packet envelope, removes the two checksum bytes, and dispatches model parsing; malformed or truncated envelopes return `null`.

## Function-name packets require a valid table index

The advertised table count must be positive and each function-name index must be smaller than that count. Reject invalid entries before callers allocate/index the accumulated name list.
