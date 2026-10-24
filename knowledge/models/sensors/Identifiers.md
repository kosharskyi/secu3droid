## Sensor enum names are part of the persisted card configuration

Room stores `GaugeType` and `IndicatorType` names in `gauge_state.gauge_type` and `indicator_state.indicator_type`. Preserve existing constants when changing presentation; new readouts must not rename an existing type to repurpose its saved configuration.

## Instantaneous and cumulative fuel consumption use different packet fields

Fuel consumption in L/100 km reads `inj_ffd`, while `cons_fuel` belongs to the separate cumulative-litre counter.
