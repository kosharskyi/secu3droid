## Toolbar connection state uses one fixed, accessible car slot

Toolbars keep the ECU state in a 48 dp slot with a 40 dp car glyph; the full localized state is exposed as its content description and on long press, so translations and font scale cannot widen or truncate the toolbar. Keep status colors and labels shared across toolbar surfaces. Home may replace the label with its firmware action description, while retaining the same state color and slot.

## Toolbar status precedes trailing actions by default

The shared bar places connection status before screen actions by default, with a 12 dp gap, so destinations share the same trailing toolbar order. A screen that needs the previous status-after-actions order must explicitly opt out with `statusBeforeActions = false`.

## The top app bar owns its status-bar inset

Material3 `TopAppBar` applies its status-bar window inset and paints through that area. A screen should not add a parent top inset such as `statusBarsPadding()` or the top side of `safeDrawing`, which moves the bar below the inset and leaves the system-bar area with the parent background color. Other safe-drawing sides can still be applied to screen content.
