# Compose screen and navigation conventions

- Use Navigation Compose for new or migrated destinations. Define routes in a Compose `NavHost`; screen composables report navigation intent through callbacks instead of owning a `NavController`.
- The ViewModel owns screen state and user actions. Render immutable state and report user intent through callbacks. During the migration, observing LiveData with `observeAsState()` is acceptable; use lifecycle-aware Flow collection for new Flow state.
- Recomposition must remain safe to repeat: do not send ECU commands, navigate, show dialogs, or perform other one-shot effects from the body of a composable. Handle those actions from explicit callbacks or lifecycle-aware effect APIs when needed.
- While a legacy Fragment still hosts Compose content, configure its `ComposeView` with `DisposeOnViewTreeLifecycleDestroyed` before `setContent` so the composition follows the Fragment view lifecycle.
- Use `SecuComposeTheme` and shared components in `ui/compose` for new screens. Preserve existing screen appearance during incremental migration unless a redesign is requested.
- Every rewritten screen must include relevant Compose UI tests. Cover rendered persisted state, key user interactions and callbacks, validation or dialogs, and navigation/link semantics where applicable; add a persistence integration test when the screen writes durable preferences.
