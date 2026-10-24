## S3L sensor flags have fixed record padding positions

The 252-byte little-endian S3L record stores airflow at byte 32, one padding byte at 33, the main and additional flag words at 34 and 36, then two padding bytes at 38–39 before TPS at 40. Preserve these positions in the record format; readers ignore the padding values. The app writes `0xCC` as deterministic filler.

## Logger I/O failures stop the singleton session and remain pending until acknowledged

Binary and CSV sinks are serialized with start and stop. An I/O failure clears the active session and publishes a monotonically increasing pending error ID; the Sensors route acknowledges only the ID it has displayed, so an error raised while that route is inactive can be shown when it resumes.

## Log deletion must share the session lock with writes

The logs screen delegates deletion to `SecuLogger`, which compares the target with the active file and deletes while holding the same lock used by prepare, start, write, and stop. Keeping the check and delete in that critical section prevents a prepared log from becoming active between the guard and filesystem operation. A rejected active-file deletion leaves the session and pending mark untouched.

## Idling VE is logged as service flag bit 9 from additional flag bit 0

The ECU packet keeps Idling VE in `additionalFlags` bit 0, separate from `serviceFlags`. CSV `ServFlag` and the S3L service word use bit 9 for it, so the logger derives a copy of the service word with bit 9 set or cleared from `corrIdlve`; it must not modify the parsed packet.

## Log marks keep latest-click-wins replacement semantics

The app uses latest-click-wins mark behavior. Each mark has its own bit, but only the last clicked mark is retained for the next sensor record. Explicit stop and each valid new start clear a pending mark so it cannot cross logger-session boundaries. Starting an already-running session remains a no-op and preserves its pending mark.

## Sensor packets always reach the logger's preference gate

The connection manager forwards each sensor packet even when logging is disabled. `SecuLogger.logPacket` owns the preference check so it can stop and close an active session when the setting is turned off; filtering packets in the connection manager would bypass that cleanup.
