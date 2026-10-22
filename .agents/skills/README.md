# Installed Android skills

These skill packages are copied from the official [`android/skills`](https://github.com/android/skills) repository at revision `95ea931a1ad8f9b2be132ae0cfb2aad964d71118`.

| Installed folder | Upstream path |
| --- | --- |
| `android-cli` | `devtools/android-cli` |
| `adaptive` | `jetpack-compose/adaptive` |
| `jetpack-compose-performance` | `jetpack-compose/jetpack-compose-performance` |
| `migrate-xml-views-to-jetpack-compose` | `jetpack-compose/migration/migrate-xml-views-to-jetpack-compose` |
| `anr-audit-and-fix` | `performance/anr-audit-and-fix` |
| `r8-analyzer` | `performance/r8-analyzer` |
| `android-profiler` | `profilers/android-profiler` |
| `android-intent-security` | `security/android-intent-security` |
| `android-permissions-security` | `security/android-permissions-security` |
| `edge-to-edge` | `system/edge-to-edge` |
| `testing-setup` | `testing/testing-setup` |

The package contents and support directories are preserved from upstream. Each folder also contains the upstream root `LICENSE.txt` (Apache-2.0), because the skill instructions refer to that file locally.

To update these packages, choose and record an immutable upstream commit, then run the repository's skill-installer helper with `--repo android/skills --ref <commit> --dest /home/vitalii/dev/Secu3droid/.agents/skills --path` followed by all upstream paths in the table. The helper refuses to overwrite existing folders, so remove only the selected installed folders before reinstalling. Copy the new upstream root `LICENSE.txt` into every installed folder and update this revision after verifying the resulting contents.
