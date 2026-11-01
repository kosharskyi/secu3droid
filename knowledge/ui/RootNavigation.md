## Compose and system bars must use the same resolved preference

`RootNavigation` resolves System/Light/Dark and passes that value to both `SecuComposeTheme` and `MainActivity.applyAppearance`. XML night resources alone cannot reflect a user's forced appearance preference.

## Android 7 requires a dark navigation bar even in light appearance

Dark navigation-bar icons require Android 8.0 (API 26). On Android 7.0/7.1 the navigation bar must keep a dark background so the platform's light icons remain visible. Dark status-bar icons are already supported there, so the status bar can still have a light background. The `isAppearanceLightNavigationBars` API refers to a light background, not light-colored icons.

## USB reconnects automatically while the app is visible

Attach and detach broadcasts remain observed while any route or dialog is visible. The Activity forwards attach events and asks `Secu3ConnectionManager` to synchronize attachments on resume; `UsbConnection` owns authoritative snapshots, support checks, permission de-duplication and unlimited open/handshake retries. Snapshot query failure returns `null` without being treated as an empty list. Synchronization returns only a permission request when Android must show a prompt. USB reports `Connected` only after firmware confirmation; Bluetooth reports it after socket establishment. Both use the same Home navigation path.

The manager publishes `UsbAwaitingEcu` after five seconds without a valid ECU response, both during initial handshake and after a confirmed USB session loses liveness. Map this state to connecting in shared indicators; StartScreen replaces the generic attachment hint with the localized ignition prompt. Initial handshake timeout does not trigger navigation. An established-session loss first emits `Disconnected`, then retry progress, so the Activity can return from any route to Start without interpreting transport/session history. A later cable detach emits a fresh `Disconnected` even after the earlier ECU loss was already handled.

USB lock is a computed manager snapshot. Read it when mapping connection-state events; confirmation is set or cleared before those events, so Home and Start do not need a separate lock stream.

## Foreground connection service policy belongs to the connection manager

The Activity forwards actual foreground/unlocked visibility and observed connection states; it does not inspect transport type or start/stop the service. `Secu3ConnectionManager` applies the shared background-preference rules, while `ConnectionServiceController` issues Android service commands. Home navigation and Activity recreation must not independently stop the service; an accepted user disconnect goes through the manager so its transport and logger cleanup stays coordinated.

While the Activity is resumed, destination changes also forward a neutral synchronization event to the manager. This applies preference changes when returning from Settings without making service policy depend on Home. The one `wakelock` preference governs both USB and Bluetooth. Turning it off stops only the service while the Activity is visible; a real Activity stop ends transport and logging and publishes `Disconnected`. Configuration recreation is ignored. With the option on, both transports remain active and service-owned through retries.

## Notification access stays a Settings-only user action

The Settings route asks for notification permission only after the user enables background work or selects the connection-notifications row. Rationale and permanent denial handling stay in Compose, while grant state is checked from Android again on resume. Notification request dialogs normally pause the Activity without stopping it; a real Home/lock transition still reaches the manager's background policy. A permission grant refreshes the existing service notification in place without restarting the connection.

## Android's attach chooser can grant USB access after the explicit permission result

The system may ask to open an already visible app independently of its `requestPermission` dialog. A denied permission result releases the one pending prompt slot but retains that attachment's prompt history. Recheck platform permission on a new attach intent and on resume; once access is granted, the manager retries open and firmware handshake without an attachment-level attempt limit. The permission request token and payload are owned together by `UsbConnection`, while detach or resume reconciliation invalidates missing attachments.

## Connection admission follows the live Start destination

The Activity refreshes a route-neutral manager capability from the actual NavController destination before handling USB attach/permission callbacks or starting Bluetooth. New USB and Bluetooth initiation is allowed only while Start is the active foreground, unlocked route. Attach events arriving elsewhere are deferred until Start; route entry and resume reconcile the attached-device snapshot. Snapshot reconciliation still runs on feature routes so missed detach and identity replacement clean up active USB resources even when starting a replacement is disallowed. Manager admission defaults closed until the Activity confirms Start, covering attach intents that arrive before the NavHost exists.

Refresh admission synchronously after navigation as well: Compose's destination effect runs later, so waiting for it can leave Start admission enabled briefly after opening Settings. Always read the controller's live destination rather than a captured destination from an earlier effect.

## Connection navigation uses current state and route-aware automatic requests

`MainActivityViewModel` observes status changes for its lifetime but only evaluates navigation while the Activity is resumed and unlocked. After pause it reacts to the manager's latest state, so an intermediate background disconnect followed by recovery does not replay stale routes. Automatic Connected-to-Home navigation is accepted only on Start, while explicit user navigation remains unrestricted. BT Reconnecting leaves the current destination intact; USB established retry returns to Start through the manager's current owner/session snapshot. StartRoute must not independently navigate on Connected.

## Drain Activity navigation queues on the Lifecycle ON_RESUME event

Android calls `onPostResume()` before `ActivityPostResumed` moves AndroidX Lifecycle to `RESUMED` on API 29+. Queued navigation and USB-intent delivery are gated on that state, so drain them from a lifecycle `ON_RESUME` observer; draining in `onResume()` or `onPostResume()` can leave permission-result work pending until another navigation callback occurs.

## Activity UI messages use destination-scoped queue entries

`UiMessageQueue` is presentation-only and is consumed by `MainActivity` while resumed. A non-null scope is the active Navigation Compose destination route; route changes discard messages for the previous route and reject late producers for inactive routes. A null scope is global and survives navigation. Queue entries survive Activity recreation through the Hilt singleton, but are acknowledged once `Toast.show()` is submitted; Android does not report whether a Toast was actually seen.

## Settings must load before themed navigation starts

RootNavigation waits for the first successful UserPrefs snapshot before choosing the app appearance or creating the navigation host. An initial storage failure shows an error while UserPrefs retries automatically; failures after startup keep the current appearance and routes visible with the same error until a read succeeds. Do not add a manual recovery action or seed the app theme from a duplicate preference default.
