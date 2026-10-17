## Keep primary gauges transparent over the dashboard surface

Theme changes must not wrap the central gauges in their own cards. The arc and value remain directly on the dashboard background in both light and dark modes; theme colors only affect the arc, ticks, and text.

## Give the unfilled gauge track contrast against the resolved background

On a light resolved background, blend the theme outline 18% toward the background so the track stays visible without looking too dark. Keep `surfaceVariant` for dark backgrounds. Keep active arc thresholds, tick colors, and all other palette roles unchanged; infer appearance from the resolved Material background color rather than system appearance.

## Weighted dashboard regions need explicit spacing

The primary gauge region expands to consume remaining height, so `SpaceBetween` cannot also guarantee separation from the status bar and secondary readouts. Keep explicit vertical gaps between those regions and within the narrow stacked gauges; landscape gauge and secondary-tile spacing remain their own layout contracts.

## Layout changes preserve sensor value and availability semantics

Gauge and readout layout must leave the existing value formatting, units, thresholds, missing-data placeholders, valid zero values, and connected-state gating unchanged.

## Do not infer load units without the ECU load-source configuration

The dashboard receives load and MAP as separate packet values, while firmware can derive load from MAP, TPS, synthetic load, or MAF. Equal load and MAP values therefore do not establish a unit; the dashboard must preserve the received load value without adding a percent label, clamping it, or requesting more ECU configuration. Keep the blank unit line in the load tile so all readout tiles retain consistent height. See the firmware's [load source selection](https://github.com/ashabelnikov/secu3app/blob/master/sources/funconv.c).
