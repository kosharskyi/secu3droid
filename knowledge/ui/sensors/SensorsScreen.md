## Configured sensor cards are independent of connection state

Room-configured cards and their order remain available while disconnected. The modern gauge keeps its configuration but displays an em dash when there is no reading; indicators must communicate unavailable state rather than a false inactive value. A null gauge reading means no needle, not a zero reading. Legacy/raw sensors must receive a null packet while disconnected so stale readings are not presented as live.

## Preserve legacy gauge range colors

Legacy gauge section colors encode ECU ranges. Theme the dial text, needle, and background while preserving those range colors.

## Gauge view modes keep one card footprint

Numeric and dial gauges share the same title area, square body, and reserved footer lines. Gauge rows size to their tallest wrapped title and stretch shorter cards to that height. The numeric value and unit sit together in the square body; transparent footer text keeps the card height equal to dial mode without adding blank accessibility text. Keep this shared footprint when changing either rendering so mixed rows do not leave gaps.

## Raw ADC values retain their packet formatting and units

Raw ADC readouts preserve packet order and three-decimal formatting inside the outlined parameter-style group. The packet fields are already voltage-scaled, so every raw field displays the localized volts unit beside its value, including fields such as TPS that are presented differently in derived sensor screens. Unavailable packets must not invent zeros.

## Indicator state is visual, with its status retained for accessibility

Modern and legacy status indicators communicate active, inactive, and unavailable states through theme-aware fills rather than visible On/Off words. Keep the matching `stateDescription` for assistive technology; disconnected indicators must use a distinct neutral appearance instead of looking inactive.

## Logger state follows the singleton session, including background I/O failures

The logger may stop while packet writes run outside the Sensors route. The ViewModel must observe the logger's state flow instead of relying only on start/stop button snapshots. A pending error ID remains until the resumed route displays and acknowledges that exact ID, so navigation away does not discard the error or replay an already acknowledged message.
