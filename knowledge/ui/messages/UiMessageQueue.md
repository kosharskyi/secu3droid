## Toasts are submitted sequentially by the resumed Activity

Presentation code publishes resource-backed messages and immutable typed format arguments to the singleton queue; only `MainActivity` resolves strings and calls `Toast.show()`. It acknowledges each ID immediately after submission and waits for the Toast's nominal duration, extended with Android's recommended accessibility timeout on API 29+, before submitting another. This pacing is an estimate because the Toast API does not report when display ends. Lifecycle pause cancels the pacing delay and the Activity cancels the submitted Toast; normal pacing completion does not cancel it. The platform provides no visibility acknowledgment, so acknowledgment records submission rather than confirmed display.

## Route-scoped messages expire and do not follow navigation

A scope is a Navigation Compose destination route. Changing to another non-null route removes prior route-scoped messages and rejects late publication for inactive scopes; null-scoped messages are global. Activity attachment resets delivery eligibility to an unknown route while preserving pending messages until navigation restores a route. A null route clears delivery eligibility without discarding messages. Entries expire after 15 seconds using monotonic elapsed time. The queue holds at most 16 entries and drops the oldest on overflow; equivalent text resource, arguments, scope, and duration entries coalesce while pending.

## The singleton queue survives configuration recreation but not process death

The injected queue contains presentation state only and stores no Activity, View, or Context. Hilt reuses it across Activity recreation in the same process. Process death clears pending messages, and acknowledgment after `Toast.show()` represents submission rather than confirmed display.
