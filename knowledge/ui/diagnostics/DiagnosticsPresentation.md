## Channel labels are presentation aliases over the domain's indexed catalog

The channel selector prepends `NONE` at index zero to the domain family catalog. It displays SECU-3i `ECF0` as `ECF` and `O2SH_O` as `O2SH_0`; output rows keep the enum names. These aliases must not change the selected protocol index.

## Hidden output rows must not filter the selector catalog

The output list may hide `TACH_O` while its direction option is disabled, but the channel catalog retains its full family order. Filtering is presentation-only so channel IDs after TACH remain stable.

## Missing input and a zero-valued packet have different presentations

Diagnostics inputs receive a nullable packet while firmware data is unavailable. Keep that null state visible as unavailable; zeroes and false bits are legitimate readings once a packet exists. Digital input state uses a compact colored text badge and matching accessibility state description. These are read-only ECU indicators.

## Output switches remain user-command controls

The output checkboxes represent ECU commands. Preserve firmware and readiness gates, accessibility behavior, and invoke callbacks only from user interaction; do not turn them into passive status indicators.

## Firmware capability labels stay reactive

The route collects the connection manager's nullable firmware state so a reply while diagnostics is open updates input labels and capability presentation. Clearing firmware removes stale capability. Show ADD_IO3 only outside SECU-3T, and treat ADD_IO4 as unavailable unless family and TPIC8101 capability are both known and enabled; a supported value of `0.000 V` is still a real reading.
