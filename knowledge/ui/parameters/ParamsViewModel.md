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

`SecurityControl` owns the mutable Security packet, iButton parsing, command validation, immutable snapshots, and `ParameterSession` admission. The ViewModel owns the write-only name/password buffers and partial key text, clearing credentials only after admission. Key programming is allowed while the immobilizer is off, matching desktop behavior; Bluetooth credentials still require `useBt`. Security sends bypass the legacy `isSendAllowed` gate.

`StarterControl` owns Starter packet edits and live firmware admission. The ViewModel maps UI field choices to named control calls and shows the packet-sent message only when `ParameterSession` admits the packed snapshot; Starter does not participate in the legacy `isSendAllowed` gate.

`AnglesControl` owns all packet edits and its session admission; the ViewModel only maps the six numeric rows and four switches to named commands. ZERO and ZERO_WITH_OCTANE are mutually exclusive on enable, and an OCTANE callback is rejected if raw ZERO became active after the editor opened.

`TemperatureControl` owns the packet, live firmware gates, fan hysteresis, and session admission. Integer editor fields use typed integer commands; packet timer values remain fractional until an explicit whole-second edit.

`CarburControl` owns Fuel Cutoff values, command validation, live firmware gates, and session admission. The ViewModel routes each editor and selector action to a named control command, and reports a sent message only after admission; these commands bypass `isSendAllowed`.

## Fuel Injection command admission belongs to its domain control

`FuelInjectionControl` reads the manager's current firmware and sends through `ParameterSession.sendPacket`, whose Boolean result drives the packet-sent message. It does not use the legacy ViewModel `isSendAllowed` gate. The screen collects its nullable values flow directly; no second Loading/Data wrapper or packet mapping belongs in the ViewModel.


`IdlingControl` owns the IDLREG packet, named numeric/flag state, value validation, current firmware admission, and immutable snapshots. `ParamsViewModel` maps the existing P0/I0 positive and P1/I1 negative screen tags to named semantic commands; typed integer and Float entry points remain distinct. IDLREG commands bypass the legacy `isSendAllowed` flag.

`KnockControl` owns the Knock packet, named nullable values, numeric and selector-code validation, single-channel bit edits, raw-field rollback, and immutable snapshots. The ViewModel maps screen enums to its named APIs and reports only admitted commands; Knock commands bypass `isSendAllowed`. Localized selector arrays stay in the screen.

`AccelerationControl` owns the Acceleration packet, raw numeric values, selector validation, live FI/GD and field-specific admission, rollback, and packed snapshots. `ParameterPageContent` rounds only the cold multiplier's initial integer-editor value and routes accepted integers and slider values to typed control commands; it does not hold a second Loading/Data state wrapper.

`LtftControl` owns the LTFT packet, all 14 numeric edits, mode validation, live FI admission, rollback, and immutable packed snapshots. Integer RPM editors and Float fields use distinct ViewModel methods; LTFT commands do not use the legacy `isSendAllowed` gate. The screen maps localized mode labels and named domain values without a second Loading/Data wrapper.

`DbwControl` owns all ten DBW edits and immutable snapshots. The ViewModel maps DBW row enums to named Float commands and reports packet-sent only when the session admits the command; the legacy `isSendAllowed` gate is not part of DBW. Refresh and the 14-byte DBW write / 16-byte read contract stay with `ParameterSession` and the packet codec.

`FunctionsControl` receives the complete `FunctionParameters` result only after `ParameterExchange` has assembled all name fragments and settings. The ViewModel forwards named edits and protocol selector codes, and reports packet-sent only for commands admitted by `ParameterSession`; Functions commands do not use `isSendAllowed`.
