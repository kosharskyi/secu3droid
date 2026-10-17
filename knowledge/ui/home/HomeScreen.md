## Theme work preserves the accepted dashboard geometry

The original Home card sizes and placements are an explicit design constraint. Theme changes preserve the portrait dashboard/grid, landscape columns, and icon sizing. Center card labels with a full-width text area; rounded intrinsic-width measurement can report overflow despite available card width, so shrinking the layout is not an appropriate fix.

## Menu icons and labels share an appearance-specific foreground

The menu's monochrome XML and raster assets include both black and white source pixels, so tint them from the card foreground. Use `onSurface` in light appearance and the softer `onSurfaceVariant` in dark appearance for both icons and labels; keep the card fill on `surface` in both modes.

## Release reminders stay inside the scrollable release body

The release dialog's server-provided message can be long. Keep “Remind me later” in the same bounded scroll region as that message so it remains reachable on short windows and at enlarged font scales; the download and release-notes actions remain in the dialog action area.

## The ECU toolbar icon keeps its firmware action description

Home shares the fixed toolbar status slot and state colors with feature screens, but its connected accessibility label must continue to announce that firmware information opens when an update is available. Only make the slot clickable when firmware information is available.
