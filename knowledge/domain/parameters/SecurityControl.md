## iButton programming does not depend on the immobilizer flag

The desktop editor permits writing valid keys while the immobilizer is disabled. Keep key command admission independent of `useImmobilizer`; the flag remains an independently editable ECU setting.

## Credential snapshots are write-only and admission controls buffer clearing

`SecurityParamPacket.credentialsSnapshot()` temporarily mutates the received packet and redacts the credentials in `finally`. The ViewModel retains name and password text when validation fails or `ParameterSession` refuses admission, and clears it only when admission succeeds.

## Restore Security fields in place after refused sends

The received `SecurityParamPacket` carries inherited unhandled bytes that data-class `copy()` does not preserve. If session admission refuses an edit, restore only the changed scalar fields on the received instance; receive and reset remain passive.
