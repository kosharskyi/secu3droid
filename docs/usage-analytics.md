# Usage analytics

Secu3droid uses Firebase Analytics for optional usage statistics. Collection is disabled by default in release builds and can be turned on or off in Settings. Debug builds permanently deactivate Firebase Analytics.

When enabled, Secu3droid sends these custom events:

| Event | Data |
|---|---|
| `screen_opened` | Fixed app section name |
| `connection_attempt` | Bluetooth or USB |
| `connection_result` | Transport, success/failure, broad category, and whether the result describes an admitted request, retry attempt, or recovery after a previous success |
| `connection_ended` | Transport and whole-second duration summed across confirmed connected intervals |
| `log_recording_started` | `csv` or `s3l` format, once after a new log file opens |
| `log_export_result` | `success` or `failure` after an actual save to Downloads or a selected folder |

Connection duration uses monotonic timestamps at existing connection-state transitions. It counts only confirmed connected intervals, excludes initial waits and retry gaps, and is emitted only when at least one connected interval was measured while consent was enabled. Revoking consent clears any accumulated duration; re-enabling during an active connection starts a new interval. No timer or periodic task is used. A process killed before the run ends may lose its duration event.

The app does not send ECU sensor values, packets, Bluetooth addresses or names, file names, paths, URIs, log contents, or raw exception messages. Some USB open errors and user cancellations do not have a reliable typed distinction in the current transport contract, so they are not labeled as connection failures. Cancelling or denying an export before a save starts does not create a failed export event.

Firebase Analytics may also collect its standard app events and app/device information. The Android Advertising ID is disabled for Analytics and ad-personalization signals are disabled. Analytics initializes only after the saved setting is enabled. Crash reporting remains a separate Firebase service; its Analytics breadcrumb integration is disabled so Analytics does not need to initialize before the user's choice.
