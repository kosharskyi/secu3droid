## The injected catalog owns gauge and indicator presentation data

`SensorPresentationCatalog` is the single source for gauge labels, units, ranges, integer formatting, ticks, color zones, and indicator labels. Routes pass the injected catalog to both sensor screen implementations; tests that render a screen provide a concrete catalog. Keep presentation maps and formatting behavior inside this catalog instead of adding global extensions or a second indicator catalog.

## Gauge zones are normalized within each configured range

Gauge sections use normalized fractions of each configured range. Keep physical zone thresholds and colors aligned with those ranges, including the uncovered ends of the OTS and AFR-difference bands.
