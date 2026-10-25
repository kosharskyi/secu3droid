## Compose and system bars must use the same resolved preference

`RootNavigation` resolves System/Light/Dark and passes that value to both `SecuComposeTheme` and `MainActivity.applyAppearance`. XML night resources alone cannot reflect a user's forced appearance preference.

## Android 7 requires a dark navigation bar even in light appearance

Dark navigation-bar icons require Android 8.0 (API 26). On Android 7.0/7.1 the navigation bar must keep a dark background so the platform's light icons remain visible. Dark status-bar icons are already supported there, so the status bar can still have a light background. The `isAppearanceLightNavigationBars` API refers to a light background, not light-colored icons.

## USB reconnects automatically while the app is visible

Attach and detach broadcasts remain observed while any route or dialog is visible. The Activity forwards attach events and asks `Secu3ConnectionManager` to synchronize attachments on resume; the manager owns authoritative device snapshots, support checks, USB permission de-duplication, and unlimited generation-fenced open/handshake retries. Snapshot query failure is never treated as an empty device list. An attached supported USB device starts automatically on return to the app, and Home is claimed only after firmware confirms the current retry. The UI claim uses the successful attempt ID so a later retry can navigate Home after an earlier confirmation was invalidated. On matching detach, clear the stack to StartScreen.

Bluetooth retry exhaustion and USB terminal results are published by the connection manager with a session generation. The Activity returns to Start for each new terminal result and retains a background result until it can navigate; clearing the result for a replacement attempt must also remove only the queued terminal Start route. A stale replay must not redirect the user after that result was already handled.

## Foreground connection service policy belongs to the connection manager

The Activity forwards actual foreground/unlocked visibility and observed connection states; it does not inspect transport type or start/stop the service. `Secu3ConnectionManager` applies the shared background-preference rules, while `ConnectionServiceController` issues Android service commands. Home navigation and Activity recreation must not independently stop the service; an accepted user disconnect goes through the manager so its transport and logger cleanup stays coordinated.

While the Activity is resumed, destination changes also forward a neutral synchronization event to the manager. This applies preference changes when returning from Settings without making service policy depend on Home. The one `wakelock` preference governs both USB and Bluetooth. Turning it off stops only the service while the Activity is visible; a real Activity stop ends transport and logging with a typed `Backgrounded` reason. Configuration recreation is ignored. With the option on, both transports remain active and service-owned through retries.

## Notification access stays a Settings-only user action

The Settings route asks for notification permission only after the user enables background work or selects the connection-notifications row. Rationale and permanent denial handling stay in Compose, while grant state is checked from Android again on resume. Notification request dialogs normally pause the Activity without stopping it; a real Home/lock transition still reaches the manager's background policy. A permission grant refreshes the existing service notification in place without restarting the connection.

## Android's attach chooser can grant USB access after the explicit permission result

The system may ask to open an already visible app independently of its `requestPermission` dialog. A denied permission result releases the one pending prompt slot but retains that attachment's prompt history. Recheck platform permission on a new attach intent and on resume; once access is granted, the manager retries open and firmware handshake without an attachment-level attempt limit. Manager-owned tokens survive Activity recreation, while detach or resume reconciliation invalidates missing attachments.

## A confirmed replacement session supersedes an older pending return to Start

USB permission dialogs pause the Activity. Clear a queued return to Start when it is performed or when a new session generation claims Home; otherwise resume can replay the previous detach after firmware has already confirmed the replacement and strand a connected ECU on StartScreen. Claim Home only when navigation can execute, so recreation before resume does not lose the pending transition.

## Drain Activity navigation queues on the Lifecycle ON_RESUME event

Android calls `onPostResume()` before `ActivityPostResumed` moves AndroidX Lifecycle to `RESUMED` on API 29+. Queued navigation and USB-intent delivery are gated on that state, so drain them from a lifecycle `ON_RESUME` observer; draining in `onResume()` or `onPostResume()` can leave permission-result work pending until another navigation callback occurs.

## Activity UI messages use destination-scoped queue entries

`UiMessageQueue` is presentation-only and is consumed by `MainActivity` while resumed. A non-null scope is the active Navigation Compose destination route; route changes discard messages for the previous route and reject late producers for inactive routes. A null scope is global and survives navigation. Queue entries survive Activity recreation through the Hilt singleton, but are acknowledged once `Toast.show()` is submitted; Android does not report whether a Toast was actually seen.
