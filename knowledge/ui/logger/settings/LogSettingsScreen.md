## Logging preferences still come from the shared settings snapshot

The editor waits for the first successful snapshot. Format changes apply when the logger opens its next file. Hiding the Sensors controls leaves an active file open and does not affect the connection manager's automatic recording preference.

## The delimiter choice stays open while its write is pending

Close the choice on a successful write or when the user dismisses it. Report a failed write through the existing preference storage message.

## The export picker delegates URI grants before saving the selection

Canceled results and results without a URI leave the saved destination intact. Save the directory after `FileHelper` accepts the result flags and URI.
