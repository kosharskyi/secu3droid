## Dashboard intentionally requests landscape as the car's instrument cluster

Dashboard is the only screen intended to lock orientation. Keep its
`SCREEN_ORIENTATION_SENSOR_LANDSCAPE` request; the maintainer explicitly excluded
removing that lock and adding portrait support from the tablet roadmap. Platforms
can ignore the request on large displays, but that compatibility behavior is not
authorization to remove the product's landscape requirement.

## Keep primary gauges transparent over the dashboard surface

Theme changes must not wrap the central gauges in their own cards. The arc and value remain directly on the dashboard background in both light and dark modes; theme colors only affect the arc, ticks, and text.

## Give the unfilled gauge track contrast against the resolved background

On a light resolved background, blend the theme outline 18% toward the background so the track stays visible without looking too dark. Keep `surfaceVariant` for dark backgrounds. Keep active arc thresholds, tick colors, and all other palette roles unchanged; infer appearance from the resolved Material background color rather than system appearance.

## Weighted dashboard regions need explicit spacing

The primary gauge region expands to consume remaining height, so `SpaceBetween` cannot also guarantee separation from the status bar and secondary readouts. Keep explicit vertical gaps between those regions and within the narrow stacked gauges; landscape gauge and secondary-tile spacing remain their own layout contracts.

## Primary gauge motion follows connection availability

The RPM and speed arcs and highlighted ticks share normalized spring progress from `ui/compose/GaugeProgress.kt`; their digits and warning colors continue to reflect the latest live reading immediately. Dashboard targets zero when disconnected or when no finite live value is available, keeping the arc and ticks visible while they retract. Because Dashboard keeps this animation state across availability changes, restored readings retarget smoothly from the current drawn progress. Read the animation only in Canvas drawing so it does not remeasure the dashboard at frame rate.

## Layout changes preserve sensor value and availability semantics

Gauge and readout layout must leave the existing value formatting, units, thresholds, missing-data placeholders, valid zero values, and connected-state gating unchanged.

## Do not infer load units without the ECU load-source configuration

The dashboard receives load and MAP as separate packet values, while firmware can derive load from MAP, TPS, synthetic load, or MAF. Equal load and MAP values therefore do not establish a unit; the dashboard must preserve the received load value without adding a percent label, clamping it, or requesting more ECU configuration. Keep the blank unit line in the load tile so all readout tiles retain consistent height. See the firmware's [load source selection](https://github.com/ashabelnikov/secu3app/blob/master/sources/funconv.c).

## Dashboard packet values expire when the connection drops

Clear the last sampled dashboard packet as soon as the connection state leaves `Connected`. Dashboard packets stay a non-replaying flow: each route collection starts without a packet and shows a value only after a fresh sensor packet arrives. Keep the Compose initial value null so stopping and resuming the route cannot reveal the previous session's reading; preserve existing null and zero-value behavior.

## Dashboard device selection is transport neutral

Bluetooth device selection belongs to Start. Dashboard renders the common sensor packet and connection-state streams for whichever transport is active; it must not read Bluetooth preferences or warn that a Bluetooth device is missing, because a valid USB session does not require one.

## System bars must restore without awaiting preferences

Dashboard's lifecycle cleanup runs in a canceled coroutine. Restore the captured system bars synchronously; awaiting DataStore there would throw cancellation before restoration. Forward the keep-screen preference to the Activity's existing scope, which applies it against the current route and handles storage failures independently.
