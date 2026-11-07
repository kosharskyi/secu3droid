## Tick labels, section lookup, and sensor readouts use different formatting

`GaugeScale` is constructed with the concrete `GaugePresentation` owned by the screen's injected catalog. It must not look up global `GaugeType` extensions. Compact tick-label formatting belongs to the scale instance; sensor readout formatting belongs to `SensorPresentationCatalog`. Shared section lookup normalizes a finite value, treats each section start as inclusive and its end as exclusive, and includes a normalized endpoint of 1 only for the final section. Intentional gaps return no section so consumers can render them neutrally.

## Gauge tick count includes both range endpoints

`GaugePresentation.tickCount` is the number of configured value ticks, evenly spaced from `minValue` through `maxValue`, including both endpoints. `GaugeScale` supplies tick fractions/values/angles to the Sensors renderers, which retain their configured positions across dial styles. Dashboard preserves its original eleven unlabeled decorative marks independently of `tickCount`, while using the same scale range, normalization, and section lookup. Tick labels round to the nearest integer, while tick values and angles retain their original fractional precision. Sensor readout precision is separate and is applied by `SensorPresentationCatalog.formatValue`.

## RPM tick labels are expressed in hundreds

Keep the configured 0…8000 RPM values and sections intact, while the major tick labels use the even configured indices as 0…80 and show `×100` inside the dial. This compact scale formatting applies only to tick labels; the live sensor value and `об/хв` readout retain their full units and precision.

## Injection phase display mode changes only the phase scale bounds

`BEGIN_INJ_PHASE` and `END_INJ_PHASE` keep the same normalized full-face color zone in every display mode, while their tick values and needle normalization use the selected 0…720 or −360…360 bounds. The ViewModel transforms the wire value into that same mode before formatting the readout, so renderers must not transform it again. Distance and total consumed fuel default to numeric cards because they are cumulative counters without dial ranges; their fallback dial scales are 0…1000 km and 0…1000 L, while instantaneous consumption uses 0…100 L/h.
