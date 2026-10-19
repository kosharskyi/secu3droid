## Active and saved error states are independent

An ECU error can be active and saved at the same time. Keep the two badges independent and continue listing errors when either state is set; the grouped visual treatment must not collapse the states into one inferred severity.

## Saved errors use the EEPROM operation and a cached response

Request saved ECU errors with the `u,0,2` operation packet. The `h,x` change-mode packet only selects the ECU's existing RAM transfer buffer; it does not read saved errors from EEPROM. The one-shot response can arrive immediately, so start collecting the transport flow before sending the request, then retain the received packet in LiveData for UI observers.
