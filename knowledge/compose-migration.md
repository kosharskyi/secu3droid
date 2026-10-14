# Compose screen and navigation conventions

- Use Navigation Compose for new or migrated destinations. Define routes in a Compose `NavHost`; screen composables report navigation intent through callbacks instead of owning a `NavController`.
- The ViewModel owns screen state and user actions. Render immutable state and report user intent through callbacks. During the migration, observing LiveData with `observeAsState()` is acceptable; use lifecycle-aware Flow collection for new Flow state.
- Recomposition must remain safe to repeat: do not send ECU commands, navigate, show dialogs, or perform other one-shot effects from the body of a composable. Handle those actions from explicit callbacks or lifecycle-aware effect APIs when needed.
- `MainActivity` is a `ComponentActivity` whose content is the single root Compose `NavHost`. Keep platform result launchers in routes and register/unregister platform receivers with the route lifecycle.
- Give each route its own Hilt ViewModel scoped to its `NavBackStackEntry`. Use a navigation graph entry when a group of child destinations must share one session ViewModel.
- A modal navigation destination should keep the underlying route visible and lifecycle-capped; use one dialog window and one modal surface, not nested dialog windows.
- Use `SecuComposeTheme` and shared components in `ui/compose` for new screens. All Compose UI widgets and theming use Material3 (`androidx.compose.material3`); do not use Material2 widgets or Google Material view/XML libraries. Existing `androidx.compose.material.icons` artifacts are allowed for the icon compatibility namespace. XML resources may define platform-only Activity/window themes. Preserve existing screen appearance during incremental migration unless a redesign is requested.
- Every rewritten screen must include relevant Compose UI tests. Cover rendered persisted state, key user interactions and callbacks, validation or dialogs, and navigation/link semantics where applicable; add a persistence integration test when the screen writes durable preferences.

The main navigation host starts at `START_ROUTE` and owns Start, Home, and all feature routes. Home remains a normal route under the Firmware dialog destination. Back from a feature route returns to its prior route; Home exit returns to Start and removes the Home route and its feature entries.
