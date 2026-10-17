## Toolbar connection state uses one fixed, accessible car slot

Toolbars keep the ECU state in a 48 dp slot with a 40 dp car glyph; the full localized state is exposed as its content description and on long press, so translations and font scale cannot widen or truncate the toolbar. Keep status colors and labels shared across toolbar surfaces. Home may replace the label with its firmware action description, while retaining the same state color and slot.
