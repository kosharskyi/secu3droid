## Default cards come from the packaged Room database

The initial gauge and indicator rows are part of `database/secu3droid.db`, which is created with the Room schema and supplied through `createFromAsset`. Room installs the asset only when no database already exists, so existing databases (including empty ones) remain untouched.

The packaged database must match a Room schema version that the current `AppDatabase` can open directly or through a registered migration. Its enum values must match the stored enum names. Update the asset when default seeded rows change.

## Moving sensor enums between packages does not change stored values

Room stores `models.sensors.GaugeType` and `IndicatorType` as their enum names, without the Kotlin package. Moving these types leaves the schema and packaged database compatible as long as their constant names stay unchanged.

## Packaged version 1 sensor rows migrate into version 2 mixed cards

The bundled database remains at schema version 1 so first open copies its default gauge and indicator rows before Room runs the registered migration. Version 2 adds `is_chart` with default false and changes the gauge-type uniqueness constraint to a gauge-type and card-kind pair. This preserves gauge IDs, order, numeric mode, and indicator data while allowing one graph card for the same sensor.
