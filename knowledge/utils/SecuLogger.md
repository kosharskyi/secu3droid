## S3L sensor flags have fixed record padding positions

The 252-byte little-endian S3L record stores airflow at byte 32, one padding byte at 33, the main and additional flag words at 34 and 36, then two padding bytes at 38–39 before TPS at 40. Preserve these positions in the record format; readers ignore the padding values. The app writes `0xCC` as deterministic filler.

## Logger failures publish the global error message at the source

Binary and CSV sinks are serialized with start and stop. On failure, LoggerSession clears the active session before synchronously asking SecuLogger to publish the existing localized global error through UiMessageQueue. Do not add a parallel pending-ID acknowledgment path in the ViewModel; MainActivity already owns resumed Toast delivery.

## Logger controls, packet writes, and deletion share one background FIFO

The singleton logger serializes starts, stops, marks, packet writes, and deletion on a worker that outlives screens and the foreground service. UI controls enqueue without waiting; packet collection awaits its write to prevent an unbounded backlog. Start selects and opens the file as one command, and deletion awaits the worker's result so its active-file check and filesystem operation cannot race a queued start or write. A rejected active-file deletion leaves the session and pending mark untouched.

## Idling VE is logged as service flag bit 9 from additional flag bit 0

The ECU packet keeps Idling VE in `additionalFlags` bit 0, separate from `serviceFlags`. CSV `ServFlag` and the S3L service word use bit 9 for it, so the logger derives a copy of the service word with bit 9 set or cleared from `corrIdlve`; it must not modify the parsed packet.

## Log marks keep latest-click-wins replacement semantics

The app uses latest-click-wins mark behavior. Each mark has its own bit, but only the last clicked mark is retained for the next sensor record. Explicit stop and each valid new start clear a pending mark so it cannot cross logger-session boundaries. Starting an already-running session remains a no-op and preserves its pending mark.

## A file's format preferences are read once when it opens

Read the format, delimiter, and CSV header choice from one settings snapshot in the start command. A settings read failure aborts that start and publishes the global logger error. The open file keeps its format across subsequent preference changes. Packet writes need no preference reads.

## All callers use the same start and stop commands

The logger does not store the origin of a start or the automatic recording preference. The connection manager decides when to call these commands for a connection, and Sensors calls them for manual controls. After a stop or failure, packets alone cannot reopen a file.
