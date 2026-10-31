## Release tags must pass the reusable Android CI workflow

The release workflow calls `android-ci.yml` before it restores signing material or builds a release APK. Its gate is the reusable workflow's debug build, unit tests, and lint; keep that workflow free of release secrets because debug checks use the tracked `app/src/debug/google-services.json`, and the debug manifest removes `FirebaseInitProvider`.

## The compile SDK package ID differs from Gradle's integer API level

The project compiles against integer API level 37 with build tools 36.1.0. The SDK package ID for that compile platform is `platforms;android-37.0` even though Gradle's `compileSdk` remains `37`.

## Android SDK setup must explicitly request platform-tools

Keep `packages: 'platform-tools'` explicit on each `android-actions/setup-android` invocation so the SDK package request does not depend on the action's default list. The action also provisions the command-line tools used by the separate compile SDK and build-tools installation step.

## Node-based actions require a sufficiently recent runner independently of the JDK

The checkout, Java, Gradle, artifact, and Android SDK actions use Node 24 even though Gradle is launched with JDK 17. Node 24 requires a self-hosted Actions runner at version 2.327.1 or newer; changing the workflow JDK does not affect action runtime compatibility.

`upload-artifact@v5` mentions preliminary Node 24 support in its release notes, but its action manifest still declares `runs.using: node20`. Check the manifest runtime declaration rather than release-note wording; v6 declares Node 24.

## Gradle selects its daemon JVM independently of the workflow launcher JDK

The workflow's JDK 17 launches the wrapper, but `gradle/gradle-daemon-jvm.properties` selects JetBrains JDK 21 for the daemon. A fresh runner may download that toolchain; changing `setup-java` alone does not override the checked-in daemon criteria.

## Local instrumented tests require Bluetooth and relaxed hidden API checks

The instrumented suite includes discovery lifecycle coverage that assumes Bluetooth is enabled, plus a diagnostics integration test that reflects into `BluetoothSocket` internals. When running it locally, enable Bluetooth and set `hidden_api_policy`; preserve animations because the Compose motion test asserts intermediate states. Instrumented tests remain available for local runs but are not part of automated CI.
