## Gauge tick count includes both range endpoints

`GaugeType.tickCount` is the number of configured value ticks, evenly spaced from `minValue` through `maxValue`, including both endpoints. `GaugeScale` is derived from `GaugeType` and is the shared source for tick fractions/values/angles, section ranges, clamped normalization, and tick-label formatting across classic and modern dial styles. Renderer-specific label visibility and line geometry must not create additional scale positions.
