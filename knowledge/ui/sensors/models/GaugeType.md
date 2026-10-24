## Gauge zones are normalized within each configured range

Gauge sections use normalized fractions of each configured range. Keep physical zone thresholds and colors aligned with those ranges, including the uncovered ends of the OTS and AFR-difference bands.

## Gauge enum names are part of the persisted card configuration

Room stores `GaugeType` names in `gauge_state.gauge_type`. Preserve existing constants when adjusting ranges or units; new readouts must not rename an existing type to repurpose its saved configuration. Fuel consumption in L/100 km reads `inj_ffd`, while `cons_fuel` belongs to the separate cumulative-litre counter.
