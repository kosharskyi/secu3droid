## Tick labels and sensor readouts use different formatting

`GaugeScale` is constructed with the concrete `GaugePresentation` owned by the screen's injected catalog. It must not look up global `GaugeType` extensions. Compact tick-label formatting belongs to the scale instance; sensor readout formatting belongs to `SensorPresentationCatalog`.

## Gauge tick count includes both range endpoints

`GaugePresentation.tickCount` is the number of configured value ticks, evenly spaced from `minValue` through `maxValue`, including both endpoints. `GaugeScale` receives the catalog's concrete description and is the shared source for tick fractions/values/angles, section ranges, and clamped normalization across classic and modern dial styles. Tick labels retain up to three meaningful fractional digits so dense tick scales remain accurate; renderer-specific label visibility and line geometry must not create additional scale positions. Sensor readout precision is separate and is applied by `SensorPresentationCatalog.formatValue`.

## Injection phase display mode changes only the phase scale bounds

`BEGIN_INJ_PHASE` and `END_INJ_PHASE` keep the same normalized full-face color zone in every display mode, while their tick values and needle normalization use the selected 0…720 or −360…360 bounds. The ViewModel transforms the wire value into that same mode before formatting the readout, so renderers must not transform it again. Distance and total consumed fuel default to numeric cards because they are cumulative counters without dial ranges; their fallback dial scales are 0…1000 km and 0…1000 L, while instantaneous consumption uses 0…100 L/h.
