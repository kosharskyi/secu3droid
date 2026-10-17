# Compose UI knowledge index

These notes record cross-screen constraints and platform quirks that are easy to miss during UI changes:

- [Shared Compose theme](ui/compose/SecuComposeTheme.md): content-color provision and nested appearance inheritance.
- [Tab row and pager](ui/compose/SecuTabRow.md): visual swipe selection versus settled ECU page activation.
- [Parameter forms](ui/parameters/compose/ParameterControls.md): shared row geometry, compound value tags, selector accessibility, and scroll ownership.
- [Accepted Starter appearance](ui/parameters/pages/compose/StarterScreen.md): accepted numeric and switch row reference.
- [Accepted Functions appearance](ui/parameters/pages/compose/FunctionsScreen.md): accepted selector reference and selector accessibility contract.
- [Parameter page constraints](ui/parameters/pages/AdvancedPages.md): manual control cancellation, field availability, credentials, and protocol selector codes.
- [Root navigation and system bars](ui/RootNavigation.md): one resolved appearance across Compose and platform bars.
- [Home screen](ui/home/HomeScreen.md): accepted dashboard geometry and monochrome menu assets.
- [Start button](ui/start/CircularButton.md): visible progress stroke and translated-label fit.
- [Sensors](ui/sensors/SensorsScreen.md): disconnected reading semantics and ECU range colors.
- [Appearance preferences](utils/UserPrefs.md): new-key and legacy-key migration precedence.
