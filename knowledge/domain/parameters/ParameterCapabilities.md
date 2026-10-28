## Capabilities are immutable snapshots of decoded firmware facts

Construct a new snapshot from the current nullable `FirmwareInfoPacket`; do not retain the mutable packet or rebuild individual rules in UI or ViewModel consumers. Null firmware is intentionally permissive for field availability and editor previews, but optional tabs remain hidden except Choke at its historical base-list position. Lambda channel selection remains false until firmware is known; once known, fuel injection, gas dose, or CARB_AFR support enables channel selection. Lambda STOICH has the narrower rule: only fuel injection or gas dose enables it. CARB_AFR-only firmware supports Lambda channels without enabling STOICH.

## Shared field rules accept only the changing parameter values

Capability methods take the current missing-teeth value, throttle-as-map state, merged-output state, or acceleration enrichment mode where availability depends on an editable parameter. They do not own screen enums, localized resources, CKPS TDC geometry, or packet mutation. Keep unknown incoming values visible and do not clamp them to the currently supported choices.

The firmware's option bit 2 is shared with the desktop controller's ODDFIRE_ALGO flag: it disables CKPS teeth-before-TDC selection and enables seven-cylinder choices. Preserve both interpretations.

## Page labels and order stay in the UI while support stays here

`ParameterPageSpec` owns localized titles and the stable page order; this class decides whether a `ParameterPage` is supported. The ViewModel derives one snapshot from the manager's current firmware flow, and the route passes that same snapshot to page rendering and page-list filtering so an ECU change updates both consistently.
