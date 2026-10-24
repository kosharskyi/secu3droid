## Default cards come from the packaged Room database

The initial gauge and indicator rows are part of `database/secu3droid.db`, which is created with the Room schema and supplied through `createFromAsset`. Room installs the asset only when no database already exists, so existing databases (including empty ones) remain untouched.

The packaged database must keep the same Room schema identity and version as `AppDatabase`, and its enum values must match the stored enum names. Regenerate it through Room if entities or seeded enum values change.

## Moving sensor enums between packages does not change stored values

Room stores `models.sensors.GaugeType` and `IndicatorType` as their enum names, without the Kotlin package. Moving these types leaves the schema and packaged database compatible as long as their constant names stay unchanged.
