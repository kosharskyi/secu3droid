## Keep each action bound to the file represented by its stable list key

Log rows are keyed by `absolutePath`, while share, save, and delete callbacks receive the original `File` instance for that row. Presentation changes must preserve that binding; filenames and formatted sizes are display-only and must not be used to identify a log.

## Deletion results come from the logger session owner

Delete confirmation remains pending-state-backed, but the ViewModel must pass the selected path to `SecuLogger` for the active-file check and deletion. The logger serializes that operation with writes, so the screen should report active-file rejection and filesystem failure separately and refresh the list after either result.
