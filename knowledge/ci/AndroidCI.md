## Release tags must pass the reusable Android CI workflow

The release workflow calls `android-ci.yml` before it restores signing material or builds a release APK. Keep that reusable workflow free of release secrets: its debug checks use the tracked `app/src/debug/google-services.json`, and the debug manifest removes `FirebaseInitProvider`.

## Android compile SDK and emulator API levels are independent

The project compiles against integer API level 37 with build tools 36.1.0, while connected tests run on an API 36 x86_64 emulator. The SDK package ID for that compile platform is `platforms;android-37.0` even though Gradle's `compileSdk` remains `37`. Keep both SDK packages installed in CI; changing the emulator API does not change the app's compile SDK requirement.

## Android setup action v3 must explicitly request platform-tools

The `android-actions/setup-android@v3` action defaults to installing `tools platform-tools`, but the obsolete `tools` package can fail SDK package resolution on current runners. Set `packages: 'platform-tools'` on each invocation; the action still provides command-line tools used by the separate compile SDK and build-tools installation step.

## Gradle selects its daemon JVM independently of the workflow launcher JDK

The workflow's JDK 17 launches the wrapper, but `gradle/gradle-daemon-jvm.properties` selects JetBrains JDK 21 for the daemon. A fresh runner may download that toolchain; changing `setup-java` alone does not override the checked-in daemon criteria.

## Instrumentation diagnostics must be captured before emulator teardown

`android-emulator-runner` tears down the emulator when its script exits, including after a failed Gradle run or emulator preparation. Keep setup and Gradle execution in the same shell, with an exit trap installed before setup, so the always-uploaded artifact retains logcat and system dumps and the original failure status.

The action executes each input line in a separate shell. Keep `script` as a folded YAML scalar (`>-`) so the setup, trap, and Gradle command execute together.

## Instrumented tests require an enabled emulator Bluetooth adapter and relaxed hidden API checks

The connected suite includes discovery lifecycle coverage that assumes Bluetooth is enabled, plus a diagnostics integration test that reflects into `BluetoothSocket` internals. Enable and verify Bluetooth and set `hidden_api_policy` before running the suite; preserve animations because the Compose motion test asserts intermediate states.
