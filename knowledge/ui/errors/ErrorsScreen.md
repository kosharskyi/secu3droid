## Active and saved badges remain independent

An ECU error can be active and saved at the same time. Keep the two badges independent and continue listing the error when either state is set; do not infer one state from the other.

## Connection state gates actions immediately

The domain snapshot owns clear and retry capabilities, while the UI also gates both actions on the current connection state so stale snapshot presentation cannot expose an action during disconnect. The clear confirmation is UI state and is dismissed when the route pauses or the connection event stream reports loss.
