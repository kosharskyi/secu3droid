## Keep the status collector alive while navigation is paused

The ViewModel maps the manager's ordered status stream to one pending route. Its collector must survive Activity pause and configuration recreation: a brief `Disconnected` followed by USB retry progress still requires Start, while a later `Connected` replaces it with Home. Re-subscribing from the Activity would replay the last Connected/Disconnected and could interrupt a feature route after that transition was already handled.

## A pending route is acknowledged only after navigation becomes possible

Wait for a resumed, unlocked Activity and a ready NavHost before consuming the route. Connection transitions and ordinary user navigation share this queue, so a stale Activity-local route cannot override recovery. Intermediate retry states discard pending Home but preserve pending Start; they are not evidence that a new usable ECU session exists.
