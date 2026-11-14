## Capture packets from the active attempt without changing ECU mode

The session subscribes through the request-scoped packet overload to fence off stale transport attempts. SensorsViewModel and its task gate own ECU mode selection, including reissuing the selected tab's task after reconnect. Capture every processed-sensor packet before publishing snapshots at the display cadence, so brief spikes remain in the 10-second window without rebuilding chart series for every packet. Sample time is monotonic and the 4,096-point cap bounds dense bursts. Suspending the modern Sensors route or selecting Raw clears the trace.

## Capture injection phase in its wire convention

Keep the history in the ECU's phase convention. Apply the current display convention to the full visible window so an existing trace can never contain mixed conventions.
