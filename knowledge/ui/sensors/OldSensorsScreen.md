## The legacy table uses the shared sensor presentation catalog

`OldSensorsScreen` receives the injected `SensorPresentationCatalog` from the sensors route, so numeric precision and units match modern sensor cards. Keep its fixed row labels and packet field mapping local to the legacy layout; only presentation metadata and formatting are shared.

## Original sensor row indexes and tags remain stable

The first 43 `old-sensor-row-N` indexes follow the legacy order and are consumed by UI tests and automation. Append newly covered readouts after index 42; correcting a label, unit, or source does not move its existing row.

## Legacy injection phases are display-transformed from wire values

`injTimBegin` and `injTimEnd` remain ECU values in the 0…720 phase convention. Apply the selected `InjectionTimingMode` only while formatting the legacy readout; never write transformed values back to the packet or use them in the modern gauge model.
