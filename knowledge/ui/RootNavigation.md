## Compose and system bars must use the same resolved preference

`RootNavigation` resolves System/Light/Dark and passes that value to both `SecuComposeTheme` and `MainActivity.applyAppearance`. XML night resources alone cannot reflect a user's forced appearance preference.

## Android 7 requires a dark navigation bar even in light appearance

Dark navigation-bar icons require Android 8.0 (API 26). On Android 7.0/7.1 the navigation bar must keep a dark background so the platform's light icons remain visible. Dark status-bar icons are already supported there, so the status bar can still have a light background. The `isAppearanceLightNavigationBars` API refers to a light background, not light-colored icons.

## USB initiation belongs to Start while detach reconciliation remains global

Start observes USB attach events and issues connection commands through its ViewModel. Entering or resuming Start scans the current device list, so a cable attached on another route is discovered on return without retaining out-of-route attach events. Activity observes detach broadcasts on every route and reconciles the USB snapshot on resume/destination changes without admitting a replacement. `UsbConnection` owns authoritative snapshots, support checks, permission de-duplication and unlimited open/handshake retries. Snapshot query failure returns `null` without being treated as an empty list. Synchronization returns a permission request when Android must show a prompt. USB reports `Connected` only after firmware confirmation; Bluetooth reports it after socket establishment. Both use the same Home navigation path.

USB publishes `UsbAwaitingEcu` after an initial handshake timeout and `UsbReconnectingAwaitingEcu` after established connectivity loses liveness or its recovery handshake times out. Both show the ignition prompt; generic `UsbReconnecting` keeps the normal attachment hint. Map all three to connecting in shared indicators. Initial handshake timeout does not trigger navigation. An established loss first emits `Disconnected`, then a USB recovery status; that recovery status also returns to Start if the Activity resumes after missing the intermediate disconnect. A later cable detach emits a fresh `Disconnected` even after the earlier ECU loss was already handled.

USB lock is a computed manager snapshot. Read it when mapping connection-state events; confirmation is set or cleared before those events, so Home and Start do not need a separate lock stream.

## Foreground connection service policy belongs to the connection manager

The Activity forwards actual foreground/unlocked visibility; it does not inspect transport type or start/stop the service. `Secu3ConnectionManager` reacts directly to connection statuses and the shared background preference, while the injected `ServiceManager` issues Android commands. Home navigation and Activity recreation must not independently stop the service; an accepted user disconnect goes through the manager so its transport and logger cleanup stays coordinated.

While the Activity is resumed, destination changes also forward a neutral synchronization event to the manager. This applies preference changes when returning from Settings without making service policy depend on Home. The one `wakelock` preference governs both USB and Bluetooth. Turning it off stops only the service while the Activity is visible; backgrounding does not itself change logger policy. Automatic logging starts only after a manager-admitted connection request, survives retry progress, and ends on STOPPED. With the option on, Connected and reconnect statuses request the service; Disconnected stops it, including the intermediate USB loss status before recovery. Initial connection attempts do not request it.

## Notification access stays a Settings-only user action

The Settings route asks for notification permission only after the user enables background work or selects the connection-notifications row. Rationale and permanent denial handling stay in Compose, while grant state is checked from Android again on resume. Notification request dialogs normally pause the Activity without stopping it; a real Home/lock transition still reaches the manager's background policy. A permission grant refreshes the existing service notification in place without restarting the connection.

## Android's attach chooser can grant USB access after the explicit permission result

The system may ask to open an already visible app independently of its `requestPermission` dialog. A denied permission result releases the one pending prompt slot but retains that attachment's prompt history. Recheck platform permission on a new attach intent and on resume; once access is granted, the manager retries open and firmware handshake without an attachment-level attempt limit. The permission request token and payload are owned together by `UsbConnection`, while detach or resume reconciliation invalidates missing attachments.

## Start owns eligibility for new connection commands

The Activity reports destination and foreground changes separately to Start's ViewModel, which gates UI preparation and explicit new connection commands. The connection manager has no screen/admission state and executes commands from any caller. Leave the existing connection job to its connection manager when Start is removed; leaving the route cancels Start's unfinished pairing, permission and Bluetooth-preparation continuations. A temporary Activity background/lock makes new UI commands ineligible but preserves pending pairing, permission and location-settings continuations while Start remains the current route; results received before resume are deferred, and the pending bond is reconciled on return. Late callbacks after navigating elsewhere are ignored, and returning to Start scans USB again.

## Connection navigation uses current state and route-aware automatic requests

`MainActivityViewModel` observes status changes for its lifetime but only evaluates navigation while the Activity is resumed and unlocked. After pause it reacts to the manager's latest state, so an intermediate background disconnect followed by recovery does not replay stale routes. Automatic Connected-to-Home navigation is accepted only on Start, while explicit user navigation remains unrestricted. BT `Reconnecting` leaves the current destination intact; `UsbReconnecting` and `UsbReconnectingAwaitingEcu` return to Start using the status alone. StartRoute must not independently navigate on Connected.

## Drain Activity navigation queues on the Lifecycle ON_RESUME event

Android calls `onPostResume()` before `ActivityPostResumed` moves AndroidX Lifecycle to `RESUMED` on API 29+. Queued navigation and USB-intent delivery are gated on that state, so drain them from a lifecycle `ON_RESUME` observer; draining in `onResume()` or `onPostResume()` can leave permission-result work pending until another navigation callback occurs.

## Activity UI messages use destination-scoped queue entries

`UiMessageQueue` is presentation-only and is consumed by `MainActivity` while resumed. A non-null scope is the active Navigation Compose destination route; route changes discard messages for the previous route and reject late producers for inactive routes. A null scope is global and survives navigation. Queue entries survive Activity recreation through the Hilt singleton, but are acknowledged once `Toast.show()` is submitted; Android does not report whether a Toast was actually seen.

## Settings must load before themed navigation starts

RootNavigation waits for the first successful UserPrefs snapshot before choosing the app appearance or creating the navigation host. An initial storage failure shows an error while UserPrefs retries automatically; failures after startup keep the current appearance and routes visible with the same error until a read succeeds. Do not add a manual recovery action or seed the app theme from a duplicate preference default.

## Dashboard gauge settings temporarily releases Dashboard window policy

The picker is an ordinary Navigation Compose destination above the retained Dashboard entry. Dashboard orientation and immersive/keep-screen behavior apply only while Dashboard is active; returning resumes them. Route changes reapply the Activity's existing keep-screen preference after updating its active route, because Dashboard's lifecycle cleanup can request that preference before the new route is published. Keep the Dashboard's saveable local theme state with the retained back-stack entry so opening and canceling settings does not reset it.

## Screen analytics follows restored navigation entries

Log only fixed route names when a Navigation Compose back-stack entry becomes current. Save the last tracked entry ID so Activity recreation does not duplicate the screen event. An event can be dropped if the app reaches its first route before Analytics observes the saved consent; it must never bypass the consent gate to compensate.

## Parameter settings stays above the retained parameter destination

Open Parameter Settings as a regular destination from the Parameters toolbar and pop it to return to the same pager entry and selected tab. The route remains available while disconnected because it edits local fuel-density preferences only.
