## Background navigation reacts to the latest manager state on resume

The ViewModel collector stays alive to observe state changes and avoid replaying an already handled state after Activity recreation, but it does not decide routes while navigation is paused. On resume it evaluates the manager's current state, not a queued sequence of background transitions. USB established-session retries are identified by the manager's current USB owner/session snapshot because `UsbAwaitingEcu` can also describe an initial handshake. This keeps a background loss followed by recovery from replaying a stale Start route.

## Automatic Home is valid only from Start; manual Home is unrestricted

Tag navigation requests by whether a user explicitly requested them. Automatic `Connected` navigation is consumed only when the current destination is Start; a manual Home request from a feature route remains valid. `Disconnected` or terminal `ConnectionTimeout` returns to Start. Bluetooth `Reconnecting` does not enqueue a route, preserving the feature destination through recovery. Clear unhandled automatic requests on pause and re-evaluate the latest manager state when navigation resumes.
