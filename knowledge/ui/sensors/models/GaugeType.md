## Desktop settings defaults override instrument constructor ranges

Parity with the desktop manager uses `metScDef[0]` in `secu3man/Settings/AppSettingsModel.cpp`, applied through `MIDeskDlg::SetMetersCfg` and `SetLimits`. The initial `SetRange` in individual `MI*.cpp` files can disagree with the actual default, notably for oil temperature, torque, and AFR difference. Keep physical zone thresholds and colors aligned with those settings, including uncovered ends of the OTS and AFR-difference bands.

## Gauge enum names are part of the persisted card configuration

Room stores `GaugeType` names in `gauge_state.gauge_type`. Preserve existing constants when adjusting ranges or units; new readouts must not rename an existing type to repurpose its saved configuration. Fuel consumption in L/100 km reads `inj_ffd`, while `cons_fuel` belongs to the separate cumulative-litre counter.
