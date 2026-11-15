## DBW replies contain one receive-only word after the writable fields

The ECU reply has 16 payload bytes, while the `P` command writes 14: ten editable values followed by a Home position word only on receive. Keep Home position visible and passive, and preserve any later unknown tail after the editable bytes when sending an unrelated edit.

## DBW editing requires a reply in the current parameter session

Firmware info has no `ELEC_THROTTLE` bit. The Manager switch is local INI configuration. A decoded DBW reply supplies the Android capability for the current session; the DBW tab, manual APPS1 calibration commands and the APPS1 learning wizard require it. SECU‑3T is excluded even if a reply was previously seen.
