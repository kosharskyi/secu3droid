## Configured sensor cards are independent of connection state

Room-configured cards and their order remain available while disconnected. The modern gauge keeps its configuration but displays an em dash when there is no reading; indicators must communicate unavailable state rather than a false inactive value. A null gauge reading means no needle, not a zero reading. Legacy/raw sensors must receive a null packet while disconnected so stale readings are not presented as live.

Add, delete, and reorder operations must update the Room-driven card lists without waiting for another ECU packet. Packet arrival only refreshes each configured card's displayed reading.

## Preserve legacy gauge range colors

Legacy gauge section colors encode ECU ranges. Theme the dial text, needle, and background while preserving those range colors.

## Gauge cards resize to the active view's content

Cards keep the same column width, and each row uses the tallest card's natural outer height so wrapped titles do not leave staggered gaps between rows. Shorter cards fill that row height while their content keeps its natural dimensions; indicator content remains vertically centered. Numeric gauges use a natural-height value and unit block; classic dials use a square face, while new dials allocate enough height for scale labels outside the same full-width circular face. Classic and thin-needle dials show a value/unit footer, while the precise-arc dial puts the readout inside its face. Recompute row height from current title wrapping, gauge mode/style, font scale, and window width so a row shrinks when its content becomes shorter. Pass the measured inner card width into gauge layout rather than using `BoxWithConstraints` inside the card: rows use intrinsic height measurement, which cannot query intrinsic size through `SubcomposeLayout`.

## Gauge and indicator columns follow the available window width

`SensorsScreen` derives a shared column count from its measured content width, using a 150dp minimum cell width scaled up with accessibility font size. The configured preference no longer caps the count. Keep both grids in sync and give every cell a fixed `maxWidth / columnCount` width. Rows start at the first column even when the last row is incomplete or contains one card; full rows still fill the available width, and card contents remain centered within their cells. Width changes from rotation, split-screen, or window resizing should reflow the existing item order without persisting a new layout preference. Drag target positions come from laid-out cards, so reorder previews must continue to use those measured bounds after partial rows move to the first column.

## Raw ADC values retain their packet formatting and units

Raw ADC readouts preserve packet order and three-decimal formatting inside the outlined parameter-style group. The packet fields are already voltage-scaled, so every raw field displays the localized volts unit beside its value, including fields such as TPS that are presented differently in derived sensor screens. Unavailable packets must not invent zeros.

## Indicator state is visual, with its status retained for accessibility

Modern and legacy status indicators communicate active, inactive, and unavailable states through theme-aware fills rather than visible On/Off words. Keep the matching `stateDescription` for assistive technology; disconnected indicators must use a distinct neutral appearance instead of looking inactive.

## Logger state follows the singleton session, including background I/O failures

The logger may stop while packet writes run outside the Sensors route. The ViewModel must observe the logger's state flow instead of relying only on start/stop button snapshots. A pending error ID remains until the resumed route displays and acknowledges that exact ID, so navigation away does not discard the error or replay an already acknowledged message.

## Global dial style applies only to modern dial cards

The saved gauge style selects the dial renderer for modern configured gauges. It must not override the old sensors view or a gauge's independent numeric-view setting. Both new dial circles use the classic dial's full-width radius. Their 12sp tick values are selected from actual `GaugeType.tickCount` positions with endpoints and a central configured tick retained; a readable non-overlapping subset is shown, and major marks stay on configured tick positions. The drawing canvas can use up to 14dp of the card's existing horizontal padding for endpoint labels without changing the face radius. Precise-arc readout bounds stay inside the blue arc with 8dp clearance, and the blue inner arc remains 3dp from the outer range band. Thin-needle readout lives in a natural footer below the face. The shared 150dp minimum grid cell (scaled up with accessibility font size) keeps the precise-arc readout legible; the measured window width alone determines the number of cells.

## Thin-needle endpoints stay clear of major ticks

The rounded needle tip stops at least 2dp before major tick marks. Cap its length at `radius - 14dp` as well as 80% of the dial radius; the extra clearance accounts for the tick's inner edge and the needle's round cap.

## Dial motion animates normalized progress while readings stay immediate

Classic and both modern dial renderers share the spring in `ui/compose/GaugeProgress.kt`; numeric readouts, units, and accessibility descriptions continue to use the latest raw reading immediately. Sensors owns the animation lifecycle: the first valid reading after composition or reconnect appears at its target without sweeping from zero, while missing or non-finite data removes the pointer/progress immediately and disposes that animation state. A later reading therefore snaps directly to its current target. Read animated state from the Canvas draw callback, not from card layout, to avoid recomposing and remeasuring the card at animation frame rate. Numeric-only cards do not animate dial progress.

## All dial styles share the configured gauge scale

`GaugeScale` is derived from `GaugeType` and supplies inclusive configured ticks, range normalization, sections, angles, and shared tick-label text to all dial renderers. Renderer-specific label selection only controls which configured values fit; it must not invent scale positions. Keep PreciseArc ticks within the outer 5dp range band so they cannot close the 3dp gap to the blue inner arc; ThinNeedle marks keep their inner endpoint at `radius - 10dp` to preserve needle-tip clearance.

## Readout strings already carry the sensor's required precision

`GaugeItem.value` is formatted by `GaugeType.formatValue`; parsing it in a card is only for finite-value validation and needle positioning. Display the original valid string instead of formatting it again, or small knock voltages, injector pulse widths, and other two- or three-decimal readings lose information. Tick-label formatting remains independent from readout precision.
