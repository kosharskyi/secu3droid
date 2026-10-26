## The parameter session owns request lifecycle; the ViewModel maps results to holders

`ParameterSession` owns connection-event invalidation, page-read jobs, cache validity, and pending EEPROM-save cancellation. The ViewModel keeps editable holder state and translates typed session events into holder updates and UI messages. Close the UI send gate before resetting holders on invalidation.

## UI page resources map to domain page IDs at the screen boundary

`ParameterPageCatalog` is the injected owner of resource-title ordering and lookup. Resolve the page to its domain ID before rendering and pass that ID into `ParameterPageContent` for retry and failure matching; the session should not depend on string resources or Compose navigation state.

## The parameter exchange component owns response matching and timeout policy

The ViewModel receives typed page results from `ParameterSession`, which delegates packet matching to `ParameterExchange`. Shared subscribe-before-send ordering and response deadlines belong to `EcuExchange`; `ParameterExchange` owns Functions fragments, EEPROM acknowledgment matching, and sensor-stream restoration.

## Firmware-derived parameter availability is exposed as one snapshot

Derive a single `ParameterCapabilities` state from `Secu3ConnectionManager.firmwareInfo`. The route and page screens consume that state; do not decode firmware flags in individual consumers. GasDose command availability follows the same snapshot while its holder retains the existing pending-command cancellation and guard lifecycle.
