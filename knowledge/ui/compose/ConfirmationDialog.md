## Keep confirmation content scrollable without moving actions into it

The shared confirmation dialog is used by Start, Home, and Errors flows. Long localized messages scroll in the body while confirm and cancel remain in the dialog action area; callers continue to own callback order and dismissal state.

Compose UI tests that claim to exercise enlarged text must override `LocalDensity.fontScale`; changing only `LocalConfiguration.fontScale` does not scale Compose text in the test composition.
