## The parameter session owns request lifecycle; the ViewModel maps results to editors

`ParameterSession` owns connection-event invalidation, page-read jobs, cache validity, and pending EEPROM-save cancellation. The ViewModel translates typed session events into editor updates and UI messages. Close the UI send gate before resetting UI-owned holders on invalidation.

## UI page resources map to domain page IDs at the screen boundary

`ParameterPageCatalog` is the injected owner of resource-title ordering and lookup. Resolve the page to its domain ID before rendering and pass that ID into `ParameterPageContent` for retry and failure matching; the session should not depend on string resources or Compose navigation state.

## The parameter exchange component owns response matching and timeout policy

The ViewModel receives typed page results from `ParameterSession`, which delegates packet matching to `ParameterExchange`. Shared subscribe-before-send ordering and response deadlines belong to `EcuExchange`; `ParameterExchange` owns Functions fragments, EEPROM acknowledgment matching, and sensor-stream restoration.

## Firmware-derived parameter availability is exposed as one snapshot

Derive a single `ParameterCapabilities` state from `Secu3ConnectionManager.firmwareInfo`. The route and page screens consume that state; do not decode firmware flags in individual consumers. `GasDoseControl` and `ChokeControl` read the manager's current firmware for command admission and observe firmware changes to discard held movement; the ViewModel does not copy its presentation snapshot into either control. Choke rejects stale callbacks as soon as firmware removes Choke support, even before page navigation settles.

`CkpsControl` owns CKPS edits and reads live firmware for each command. The ViewModel only routes UI actions, maps the session's CKPS packet result into the control, and reports the control's Boolean send admission; CKPS packet mapping must not toggle the legacy `isSendAllowed` gate.

`MiscellaneousControl` similarly owns the mutable Miscellaneous packet, numeric validation, current firmware admission, and the immutable packet snapshot sent through `ParameterSession`. Its nullable values state includes the validation flag and raw hop-angle/VSS Floats; the screen rounds only for integer presentation and editors.

## Fuel Injection command admission belongs to its domain control

`FuelInjectionControl` reads the manager's current firmware and sends through `ParameterSession.sendPacket`, whose Boolean result drives the packet-sent message. It does not use the legacy ViewModel `isSendAllowed` gate. The screen collects its nullable values flow directly; no second Loading/Data wrapper or packet mapping belongs in the ViewModel.
