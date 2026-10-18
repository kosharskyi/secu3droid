## Gauge tick count includes both range endpoints

`GaugeType.tickCount` is the number of configured value ticks, evenly spaced from `minValue` through `maxValue`, including both endpoints. `GaugeScale` is derived from `GaugeType` and is the shared source for tick fractions/values/angles, section ranges, clamped normalization, and compact tick-label formatting across classic and modern dial styles. Tick labels retain up to three meaningful fractional digits so denser desktop-compatible ticks remain accurate; renderer-specific label visibility and line geometry must not create additional scale positions. Sensor readout precision is separate and comes from `GaugeType.formatValue`.

## Injection phase display mode changes only the phase scale bounds

`BEGIN_INJ_PHASE` and `END_INJ_PHASE` keep the same normalized full-face color zone in every display mode, while their tick values and needle normalization use the selected 0…720 or −360…360 bounds. The repository transforms the wire value into that same mode before formatting the readout, so renderers must not transform it again. Distance and total consumed fuel default to numeric cards because their desktop counterparts are counter readouts without dial ranges; their fallback dial scales are 0…1000 km and 0…1000 L, while instantaneous consumption uses 0…100 L/h.
