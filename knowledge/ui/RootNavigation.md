## Compose and system bars must use the same resolved preference

`RootNavigation` resolves System/Light/Dark and passes that value to both `SecuComposeTheme` and `MainActivity.applyAppearance`. XML night resources alone cannot reflect a user's forced appearance preference.

## Android 7 requires a dark navigation bar even in light appearance

Dark navigation-bar icons require Android 8.0 (API 26). On Android 7.0/7.1 the navigation bar must keep a dark background so the platform's light icons remain visible. Dark status-bar icons are already supported there, so the status bar can still have a light background. The `isAppearanceLightNavigationBars` API refers to a light background, not light-colored icons.

## Physical USB lifecycle belongs to the Activity, not StartScreen

Attach and detach broadcasts must remain observed while any route or dialog is visible. After firmware confirms a USB session, navigate to Home while clearing prior feature entries; on matching device detach, clear the stack to StartScreen. The Activity also owns the one permission request per attachment so reconnect/resume cannot prompt repeatedly.

## Android's attach chooser can grant USB access after the explicit permission result

The system may ask to open an already visible app independently of its `requestPermission` dialog. A consumed or denied permission request must not consume the one port-open attempt: recheck `hasPermission` on a new attach intent and on resume, then claim opening once per attachment. Keep both claims across Activity recreation so a duplicate intent cannot restart a confirmed or failed session.

## A confirmed replacement session supersedes an older pending return to Start

USB permission dialogs pause the Activity. Clear a queued return to Start when it is performed or when a new session generation claims Home; otherwise resume can replay the previous detach after firmware has already confirmed the replacement and strand a connected ECU on StartScreen. Claim Home only when navigation can execute, so recreation before resume does not lose the pending transition.
