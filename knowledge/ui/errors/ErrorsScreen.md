## Active and saved error states are independent

An ECU error can be active and saved at the same time. Keep the two badges independent and continue listing errors when either state is set; the grouped visual treatment must not collapse the states into one inferred severity.

## Saved errors use the EEPROM operation and a cached response

Request saved ECU errors with the `u,0,2` operation packet. The `h,x` change-mode packet only selects the ECU's existing RAM transfer buffer; it does not read saved errors from EEPROM. The one-shot response can arrive immediately, so start collecting the transport flow before sending the request, then retain the received packet in LiveData for UI observers.

## Error packets belong to a resumed, connected route session

The Errors route owns packet reads only while it is resumed and the transport is connected. Any other connection state cancels the pending saved read and clears both packets; resuming starts a fresh saved read before active errors. Observe connection events in the ViewModel so a fast disconnect/reconnect cannot be hidden by Compose observation, and fence packet updates from canceled session jobs.

## Reassert active error mode after a late firmware response

Connection setup sends the firmware-info mode request concurrently with `Connected`. If its `y` packet arrives after the saved-error response, send the active-error read again so the ECU returns to the `v` transfer mode. Keep this recovery inside the active Errors session; do not add a transport delay.
