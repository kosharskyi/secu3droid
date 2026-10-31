## Theme work preserves the accepted dashboard geometry

The original Home card sizes and placements are an explicit design constraint. Theme changes preserve the portrait dashboard/grid, landscape columns, and icon sizing. Center card labels with a full-width text area; rounded intrinsic-width measurement can report overflow despite available card width, so shrinking the layout is not an appropriate fix.

## Tablet menu layout follows the post-inset Home viewport

Resolve Home's layout from the measured menu viewport after safe drawing insets and the fixed toolbar. At a viewport width of 600dp or more and a viewport height of at least 480dp, use three columns; switch to four columns at 840dp. The Dashboard tile occupies two cells, followed by Sensors in the remaining first-row cell (and Parameters as well in the four-column layout). At heights below 480dp, use the existing compact horizontally scrollable columns so all destinations remain reachable. Keep the existing phone grid and landscape proportions unchanged.

Tablet tiles keep the full localized label and grow with the current font scale; do not truncate labels or reduce their text size to force them into a fixed-height card. Keep item order, actions, dialogs, and connection/exit behavior unchanged when changing window size.

## Menu icons and labels share an appearance-specific foreground

The menu's monochrome XML and raster assets include both black and white source pixels, so tint them from the card foreground. Use `onSurface` in light appearance and the softer `onSurfaceVariant` in dark appearance for both icons and labels; keep the card fill on `surface` in both modes.

## Release notes stay in the body while release choices use the standard footer

The release dialog's server-provided message can be long. Keep the message and “What's new” action together in the independently scrollable body; keep “Remind me later” and “Download” in AlertDialog's standard footer so the two decisions remain adjacent, with the primary action last. Material 3's standard AlertDialog adapts the footer when its labels do not fit on one row.

## The release-notes label aligns without shrinking its touch target

The “What's new” TextButton stays intrinsic width and at least 48 dp tall. Zero horizontal content padding lets its label start at the release message's leading edge without expanding the action across the body or reducing its touch target.

## The ECU toolbar icon keeps its firmware action description

Home shares the fixed toolbar status slot and state colors with feature screens, but its connected accessibility label must continue to announce that firmware information opens when an update is available. Only make the slot clickable when firmware information is available.

## USB sessions lock the Home exit controls

A confirmed USB session hides the Exit action and consumes system Back while Home is active. Feature navigation remains available, and the Activity returns to StartScreen when the active USB device is physically detached.

## Home navigates only after the manager accepts the stop

The manager rechecks the USB session lock when Home confirms an exit. If a USB handshake confirms between the UI snapshot and that request, the manager rejects the stop; keep the exit flow pending state clear and do not navigate to StartScreen. The ViewModel publishes its single UI navigation flag only when `stopConnection()` returns true. Accepted exits let the manager stop the transport, logger and foreground service as one operation.
