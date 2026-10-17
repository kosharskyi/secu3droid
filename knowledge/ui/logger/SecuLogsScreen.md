## Keep each action bound to the file represented by its stable list key

Log rows are keyed by `absolutePath`, while share, save, and delete callbacks receive the original `File` instance for that row. Presentation changes must preserve that binding; filenames and formatted sizes are display-only and must not be used to identify a log.
