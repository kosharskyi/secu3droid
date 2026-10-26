## Displayed sensor packets follow connection status, not a UI session identifier

Dashboard and Sensors observe the same non-replaying packet stream through `SensorPacketObserver`. Each observation starts with `null`, samples only while Connected, and emits `null` immediately when the connection changes; `flatMapLatest` cancels any pending sample on loss. Consumers choose their own update interval (100 ms by default). The connection manager must not cache or sample packets for these screens.
