## VSS wire distance keeps its reciprocal Float precision

The ECU stores the reciprocal `32768000 / pulsesPerKm` as a 16-bit word. Retain the parsed physical value as a Float so unrelated packet edits re-emit the original word; round only when preparing the integer UI state or explicit integer editor value.
