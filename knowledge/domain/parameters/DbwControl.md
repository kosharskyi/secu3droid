## DBW replies contain one receive-only word after the writable fields

The ECU reply has 16 payload bytes, while the `P` command writes 14: ten editable values followed by a Home position word only on receive. Keep Home position visible and passive, and preserve any later unknown tail after the editable bytes when sending an unrelated edit.

## DBW availability is the live non-SECU3T family capability

Each numeric command checks the current `ParameterCapabilities.dbw` value. Unknown firmware remains permissive for field commands; known SECU3T firmware rejects edits. The desktop `ELEC_THROTTLE` switch is a manager-local INI setting, defaulting to enabled, rather than an ECU firmware-info flag.
