## Parameter requests restore sensor streaming even when the request fails

Each parameter read restores `Secu3ReadSensors` in `finally`, including timeout or cancellation. `EcuExchange` owns the shared response subscription ordering and deadline conversion; `ParameterExchange` owns packet matching and parameter-specific recovery.

## Functions needs every name fragment before its settings packet

The names response is complete only after fragments fill every slot indexed from zero. Keep the `-1` placeholder for unseen indexes, accept fragments in any order, restore sensor streaming after the names stage, and only then request FUNSET.

## EEPROM confirmation is a distinct, single-flight operation

Only `OpCompNc.isEepromParamSave` confirms the save; the saved-engine-errors acknowledgment is unrelated. A duplicate or disconnected request returns `NotStarted`, a confirmed acknowledgment returns `Confirmed`, and response/request failure returns `Unconfirmed`. Parent cancellation must release the single-flight guard without becoming a success or failure result.
