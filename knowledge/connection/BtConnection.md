## Bluetooth preparation is owned by the cancellable transport job

Read the selected bonded MAC address before retrying, and retain it throughout that job. The suspending start method waits for preparation, propagates storage failure, and cancels only the job it started when its caller is canceled. A canceled older caller must not stop another job. New USB commands do not replace this selected connection; UI device selection and preference persistence remain in their existing owners.

## Bluetooth retries preserve their existing policy

Reread the configured attempt limit on each retry; a successful socket followed by a read failure resets the attempt counter. That established loss carries `RETRYING`, so the manager applies its retry policy without publishing `Disconnected`; `Reconnecting` remains eligible for the background service. Initial socket attempts publish `Connecting`; after a socket was established, read loss and retry progress publish `Reconnecting`. EOF must enter the read-failure path. Retry exhaustion emits `Disconnected` before the terminal ConnectionTimeout outcome. Bluetooth readiness remains socket-based; firmware bootstrap is queued before Connected and firmware information can arrive afterward.
