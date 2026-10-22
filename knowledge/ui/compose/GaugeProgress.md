## Callers own gauge animation lifecycle

`rememberAnimatedGaugeProgress` is called during composition with a normalized target and returns a reader for the current spring value. Read that returned lambda only inside the Canvas draw callback, not during composition or layout. Each screen decides what target to use and when the animation state exists: Sensors removes unavailable progress and keys valid targets by gauge type, while Dashboard targets zero through disconnects so it can smoothly retract and restore.
