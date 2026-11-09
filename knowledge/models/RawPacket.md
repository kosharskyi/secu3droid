## Raw packet parsing receives complete frames after transport checksum validation

The transport owns frame checksum validation. `RawPacket` validates the packet envelope, removes the two checksum bytes, and dispatches model parsing; malformed or truncated envelopes return `null`.

## Sensor descriptors may be deferred until calibration is available

The connection manager passes the current persisted fuel-flow calibration into this parser. A `null` calibration suppresses only SENSOR_DAT decoding; other descriptors, including firmware info, continue through the same sequential stream. Standalone callers that omit the argument retain the default of 16000 pulses per litre. See [connection](../connection/Connection.md) for preference-loading and live-update behavior.

## Error mask packets have exactly four decoded payload bytes

The `v` active-error and `x` saved-error packets carry a fixed four-byte mask. Their model parsers receive the header and descriptor after `RawPacket` removes the two frame checksum bytes, so decoded input length must be exactly six bytes; extra bytes are malformed too.

## Function-name packets require a valid table index

The advertised table count must be positive and each function-name index must be smaller than that count. Reject invalid entries before callers allocate/index the accumulated name list.

## LTFT map and axis descriptors use distinct payload contracts

The `K` LTFT map payload is a selector, an 8-bit address, and zero to sixteen signed coefficient bytes. A selector of zero selects map 0 and any nonzero selector selects map 1. Empty fragments are valid, and fragment bounds must stay within the 256-entry address space. RPM (`\"`) and load (`)`) axes each require address zero followed by exactly sixteen unsigned little-endian words; load words are divided by `LOAD_PHYSICAL_MAGNITUDE_MULTIPLIER`.
