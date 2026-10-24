## Keep each action bound to the file represented by its stable list key

Log rows are keyed by `absolutePath`, while share, save, and delete callbacks receive the original `File` instance for that row. Presentation changes must preserve that binding; filenames and formatted sizes are display-only and must not be used to identify a log.

## Deletion results come from the logger session owner

Delete confirmation remains pending-state-backed, but the ViewModel must pass the selected path to `SecuLogger` for the active-file check and deletion. The suspending delete call returns only after the logger's shared FIFO has checked and deleted the file, so the screen can report active-file rejection and filesystem failure separately and refresh the list after either result.

## Share MIME type follows the selected file's export mapping

The share intent uses the same extension-based MIME helper as all export destinations. Keep passing the original selected `File` through the callback so the MIME type and content URI both refer to that row's file.
