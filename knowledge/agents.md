# Project agent contracts

## Agent profiles supplement repository instructions and do not replace them

Profiles under `.codex/agents/` describe focused roles; repository instructions, the approved plan, and task-specific skill requirements still govern the work. Apply only skills relevant to the task, and stop for a revised plan when implementation would exceed the approved scope.

## Profiles are reusable instructions, not durable project memory

Keep profiles focused on stable role behavior. Record durable, nonprivate project invariants in the matching `knowledge/` files so future work in that component can find them.

## Android QA is a targeted validation role, not a second pass by default

Use `secu_android_qa` when a task needs reproduction or a targeted device/emulator check. Coordinate shared emulator and build use with the implementation worker, avoid repeating checks they already ran, and report environment limits such as emulator versus physical device or real ECU coverage.

## Legacy luna profile names are stable routing keys

The `luna_*` profile names are referenced by `AGENTS.md` as routing keys; they do not select the model. Model migrations should update profile model fields while preserving those names and their role descriptions.

## Agent profiles do not promise isolated sandboxes

Profiles define task guidance, not filesystem isolation or access control. Keep each handoff bounded and follow the workspace and repository permissions actually in force.
