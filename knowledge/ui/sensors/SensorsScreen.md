## Configured sensor cards are independent of connection state

Room-configured cards and their order remain available while disconnected. The modern gauge keeps its configuration but displays an em dash when there is no reading; indicators must communicate unavailable state rather than a false inactive value. A null gauge reading means no needle, not a zero reading. Legacy/raw sensors must receive a null packet while disconnected so stale readings are not presented as live.

## Preserve legacy gauge range colors

Legacy gauge section colors encode ECU ranges. Theme the dial text, needle, and background while preserving those range colors.

## Compact layouts keep configured gauges and legacy row semantics

Numeric gauge cards may change orientation to fit width and font scale, but the configured grid and per-gauge numeric/dial selection stay authoritative. Legacy/raw readouts preserve packet order and formatting inside one grouped surface; raw ADC values remain unitless, and unavailable packets must not invent zeros.

## Indicator state is visual, with its status retained for accessibility

Modern and legacy status indicators communicate active, inactive, and unavailable states through theme-aware fills rather than visible On/Off words. Keep the matching `stateDescription` for assistive technology; disconnected indicators must use a distinct neutral appearance instead of looking inactive.
