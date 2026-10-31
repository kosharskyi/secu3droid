## Bluetooth preparation is owned by the cancellable transport job

Read the selected bonded MAC address before retrying, and retain it throughout that job. The suspending start method waits for preparation, propagates storage failure, and cancels only its own generation when its caller is canceled. A USB replacement stops this job, including a preference read that has not completed yet. UI device selection and preference persistence remain in their existing owners.

## Bluetooth retries preserve their existing policy

Reread the configured attempt limit on each retry; a successful socket followed by a read failure resets the attempt counter. EOF must enter the read-failure path. Retry exhaustion emits Disconnected before the terminal ConnectionTimeout outcome. Bluetooth readiness remains socket-based; firmware bootstrap is queued before Connected and firmware information can arrive afterward.
