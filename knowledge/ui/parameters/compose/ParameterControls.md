## Form rows follow the accepted Starter and Functions references

Numeric values and switches follow [Starter](../pages/compose/StarterScreen.md); selectors follow [Functions](../pages/compose/FunctionsScreen.md). The remaining 17 parameter tabs were migrated to these patterns in this pass and have not been individually reviewed by the user. Existing dialog callbacks and ECU workflows remain caller-owned.

## Parameter tabs own their page names

The selected tab row already identifies the current parameter page. Do not repeat that name as a page heading; reserve `ParameterSection` headings for meaningful subsections within a page.

## Numeric value tags cover the number and units together

`ParameterValueRow` and `ParameterReadOnlyValueRow` preserve their established parent and `"$testTag-value"` tags. The value child exposes the locale-formatted number and units in one text leaf, even though the units use a smaller muted style. `ParameterTextValueRow` can accept caller-formatted `numberText` and `units` explicitly; callers should not parse formatted display strings to infer numeric or protocol meaning.

## Parameter screen instances own their scroll state

Each screen remembers its scroll state above its Loading/Data branch and passes it to `ParameterPage`. Fresh ECU reads temporarily render Loading; keeping the scroll state only inside the Data branch resets the user’s position. Do not share this state across pager destinations. Standalone `ParameterPage` callers may use its local default. Activation and ECU reads still follow the settled-page lifecycle contract documented in [SecuTabRow](../../compose/SecuTabRow.md).

## Editable field actions include the title and value

The button semantics and `testTag` belong to the full labeled field, so tapping either the title or the value opens the same editor or selector and accessibility can read both. The tagged parent includes its bottom divider; spacing assertions should measure from that complete control bound. A null callback on `ParameterTextValueRow` removes the button action entirely; a disabled callback remains represented as a disabled control. Selection rows retain the `${testTag}-dropdown-indicator` tag; callers may override the value child tag for legacy editor-routing contracts without changing the clickable parent tag. Optional content and state descriptions must describe the selected option, not protocol state inferred from its text.

## Sections leave spacing to their children

`ParameterSection` adds its heading and card outline but no general content inset or inter-row spacing. Shared rows provide their own horizontal inset and divider; bespoke content in a section must provide its own padding and preserve any explicit control gaps.

## Dialog styling must leave editor state and caller-owned choices intact

The shared dialog shell owns presentation only. Numeric edit text, cursor selection, changed state, and stepped fallback are keyed to the edit session and must remain inside the existing content state; selector callers continue to own option IDs, ordering, selection callbacks, and immediate dismissal. Scrollable dialog bodies keep confirmation controls reachable when the IME or a short window reduces available height.

## Parameter choices use the shared dialog and commit only on selection

Remaining enum and choice selectors use `ParameterChoiceDialog`; Functions, CKPS, Fuel Injection, and Carbur retain their equivalent existing composition from the same shared dialog parts. In both forms, callers own display fallback, ordered option IDs, and option test tags. Choosing invokes the existing callback and closes the dialog; Cancel, Back, or outside dismissal only closes it and sends no selection callback.

## Manual hold buttons share timing and cancellation behavior

Choke and Gas Dose use `ManualPositionButton`, which emits one step immediately, waits 400 ms before 80 ms repeats, and invokes periodic flush callbacks every 250 ms. It reads callbacks through `rememberUpdatedState` so recomposition does not restart an active pointer gesture. A normal release calls release once; pointer cancellation or gesture coroutine cancellation calls cancel once and discards pending movement. Page deactivation also cancels the owning controller's pending delta.
