## The parameter session owns request lifecycle; the ViewModel maps results to holders

`ParameterSession` owns connection-event invalidation, page-read jobs, cache validity, and pending EEPROM-save cancellation. The ViewModel keeps editable holder state and translates typed session events into holder updates and UI messages. Close the UI send gate before resetting holders on invalidation.

## UI page resources map to domain page IDs at the screen boundary

Use `parameterPageForTitle` when routing a resource-backed page to the domain session. The session should not depend on string resources or Compose navigation state.

## The parameter exchange component owns response matching and timeout policy

The ViewModel receives typed page results from `ParameterSession`, which delegates packet matching to `ParameterExchange`. Shared subscribe-before-send ordering and response deadlines belong to `EcuExchange`; `ParameterExchange` owns Functions fragments, EEPROM acknowledgment matching, and sensor-stream restoration.
