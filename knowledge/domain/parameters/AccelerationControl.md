## Acceleration edits retain raw fixed-point fields until explicitly changed

Cold multiplier remains a Float decoded from an unsigned 16-bit word; the whole-percent editor rounds only its initial value, while an explicit integer edit stores a Float. Balance keeps the decoded `byte / 2.56` value, including 255 as 99.609375%, until an explicit slider edit. Unrelated writes preserve these raw packet values, unknown selector codes, and packet tail.

## Acceleration command admission uses the field capability, not optional-page visibility

Every command requires the current `fuelOrGasDose` field capability. This remains permissive when firmware is unknown, matching `ParameterCapabilities`; optional-page visibility separately requires `supports(ACCELERATION)` and known FI/GD firmware. AE_TIME additionally requires fuel injection and `aeType == 1`; wallwet selection and Xtau edits require fuel injection, and Xtau also requires a positive current wallwet model.
