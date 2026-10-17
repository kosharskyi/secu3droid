## Compose and system bars must use the same resolved preference

`RootNavigation` resolves System/Light/Dark and passes that value to both `SecuComposeTheme` and `MainActivity.applyAppearance`. XML night resources alone cannot reflect a user's forced appearance preference.

## Android 7 requires a dark navigation bar even in light appearance

Dark navigation-bar icons require Android 8.0 (API 26). On Android 7.0/7.1 the navigation bar must keep a dark background so the platform's light icons remain visible. Dark status-bar icons are already supported there, so the status bar can still have a light background. The `isAppearanceLightNavigationBars` API refers to a light background, not light-colored icons.
