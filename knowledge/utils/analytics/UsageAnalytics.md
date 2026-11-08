## The Analytics API is initialized only after saved consent is enabled

The Application starts the owner, which observes DataStore first and obtains the Firebase Analytics API only after a successful `true` settings emission. The manifest default is disabled, and the AnalyticsConnector registrar is removed from Firebase component discovery so the SDK does not auto-initialize before consent. If Analytics has been initialized, revocation closes SDK collection before the DataStore write, and a pending choice suppresses stale settings snapshots while the write is unresolved. After a successful write, the owner reads the canonical preference and applies it explicitly, so an unchanged value or conflated Flow emission cannot leave collection stale. Read failures close any already-initialized SDK collection. Removing the registrar also removes Crashlytics' optional Analytics breadcrumb bridge; Crashlytics itself remains registered and reports crashes independently.

## Debug builds permanently deactivate Firebase Analytics

The debug merged manifest sets `firebase_analytics_collection_deactivated=true`, removes FirebaseInitProvider, and the runtime owner also checks `BuildConfig.DEBUG`. Saved release consent cannot enable Analytics in a debug process.

## Event payloads contain only fixed categories

Only fixed screen/transport names, result categories, CSV/S3L format, whole-second duration, and export outcome are allowed. Do not add ECU data, identifiers, paths, URIs, raw exceptions, or filenames. Firebase's standard automatic events and app/device data are separately disclosed in `docs/usage-analytics.md`.

## Connection duration is accumulated from observed transitions without a timer

The connection manager supplies admitted-run, confirmed-Connected, RETRYING, and final-stop facts. The owner uses monotonic timestamps to sum only confirmed Connected intervals. Initial waits and retry gaps are excluded. Consent revocation or a settings read failure clears accumulated time while preserving whether the current run is connected; if consent is later enabled mid-connection, a fresh interval starts then. Only a run with a measured interval emits `connection_ended`, and duplicate finalization is ignored. Durations are summed in milliseconds and converted to whole seconds once, so a confirmed sub-second session reports zero.
