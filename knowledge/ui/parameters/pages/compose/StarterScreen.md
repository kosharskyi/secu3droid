## Starter is the accepted numeric and switch reference for parameter tabs

The user accepted Starter's appearance on 2026-10-15. Numeric values use proportional tabular numerals (`tnum`) on the right, with smaller muted units; switches and values sit in one outlined card with thin row dividers. Shared parameter rows and the remaining 17 parameter tabs now follow this numeric and switch pattern; those tabs have not been individually reviewed by the user. Do not restore individual field frames or edit-pencil icons.

## Visual approval preserves the existing dialog and ECU workflow

Numeric editing still opens the existing dialog. The accepted appearance does not authorize inline editors, a Save button beside each field, or dirty/save-status indicators; those would change the interaction and ECU workflow rather than transfer the agreed visual style.

## Value semantics keep the number and units in one text node

Although the units have a smaller, muted style, the value tag still exposes the original locale-formatted number and units as one string. Editor-routing and screen tests depend on the existing `starter_<number>` clickable parent and `starter_<number>-value` child tags. Tapping the label or value continues to invoke the same dialog callback.
