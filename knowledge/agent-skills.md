# Compatibility rules for installed agent skills

## Repository instructions and task approval govern skill recommendations

Installed skill instructions provide guidance, but repository `AGENTS.md` and the user's approved task scope determine what work is allowed. A skill recommendation does not expand an approved task.

## Compose guidance must follow this project's Material3 and Navigation Compose contracts

Use Material3 widgets and Navigation Compose as required by `AGENTS.md` and `knowledge/compose-migration.md`. The adaptive skill may describe Navigation3; adopting or migrating to Navigation3 requires a separately approved task.

## Installing a skill does not authorize toolchain or app changes

The presence of Android CLI, profiling, security, or testing instructions does not itself authorize installing CLI tooling or modifying app code, dependencies, or configuration. Those changes require explicit task scope.
