## The shared theme must provide content color outside `Surface`

Material 3 does not set `LocalContentColor` from `MaterialTheme.colorScheme`. The shared theme supplies `onBackground`, which keeps default text readable on screens such as Parameters that draw directly on the background. A test wrapper that adds a `Surface` can hide a regression in this contract.

## Nested themes inherit the root's resolved appearance

A nested `SecuComposeTheme()` inherits the already-resolved root mode, including an explicit light or dark preference. It must not re-read system appearance and silently switch destinations back to the device setting.
