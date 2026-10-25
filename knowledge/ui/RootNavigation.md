## Compose and system bars must use the same resolved preference

`RootNavigation` resolves System/Light/Dark and passes that value to both `SecuComposeTheme` and `MainActivity.applyAppearance`. XML night resources alone cannot reflect a user's forced appearance preference.

## Android 7 requires a dark navigation bar even in light appearance

Dark navigation-bar icons require Android 8.0 (API 26). On Android 7.0/7.1 the navigation bar must keep a dark background so the platform's light icons remain visible. Dark status-bar icons are already supported there, so the status bar can still have a light background. The `isAppearanceLightNavigationBars` API refers to a light background, not light-colored icons.

## The Activity observes USB lifecycle while the connection manager admits attempts

Attach and detach broadcasts must remain observed while any route or dialog is visible. The Activity forwards attach and detach events and asks `Secu3ConnectionManager` to synchronize attachments on resume; the manager obtains the authoritative snapshot, owns serial support checks, active-device conflict rules, permission tokens and one open per physical attachment. Snapshot query failure must not be interpreted as an empty device list. StartScreen only presents device state and forwards manual selection. After firmware confirms a USB session, navigate to Home while clearing prior feature entries; on matching device detach, clear the stack to StartScreen.

Bluetooth retry exhaustion and USB terminal results are published by the connection manager with a session generation. The Activity returns to Start for each new terminal result and retains a background result until it can navigate; clearing the result for a replacement attempt must also remove only the queued terminal Start route. A stale replay must not redirect the user after that result was already handled.

## Foreground connection service policy belongs to the connection manager

The Activity forwards its resumed lifecycle and observed connection states; it does not inspect transport type or start/stop the service. `Secu3ConnectionManager` applies the USB-lock and Bluetooth background-preference rules, while `ConnectionServiceController` issues Android service commands. Home navigation and Activity recreation must not independently stop the service; an accepted user disconnect goes through the manager so its transport and logger cleanup stays coordinated.

While the Activity is resumed, destination changes also forward a neutral synchronization event to the manager. This applies a background-preference change when returning from Settings without making the service policy depend on the Home route; repeated transitions within feature routes must not issue repeated Android starts for the same owner generation.

## Android's attach chooser can grant USB access after the explicit permission result

The system may ask to open an already visible app independently of its `requestPermission` dialog. A denied permission result releases the one pending prompt slot but retains that attachment's attempt history. Recheck platform permission on a new attach intent and on resume; the manager allows that existing attempt to claim opening once. Manager-owned tokens survive Activity recreation, while detach or resume reconciliation invalidates missing attachments.

## A confirmed replacement session supersedes an older pending return to Start

USB permission dialogs pause the Activity. Clear a queued return to Start when it is performed or when a new session generation claims Home; otherwise resume can replay the previous detach after firmware has already confirmed the replacement and strand a connected ECU on StartScreen. Claim Home only when navigation can execute, so recreation before resume does not lose the pending transition.

## Drain Activity navigation queues on the Lifecycle ON_RESUME event

Android calls `onPostResume()` before `ActivityPostResumed` moves AndroidX Lifecycle to `RESUMED` on API 29+. Queued navigation and USB-intent delivery are gated on that state, so drain them from a lifecycle `ON_RESUME` observer; draining in `onResume()` or `onPostResume()` can leave permission-result work pending until another navigation callback occurs.
