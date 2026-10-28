## Malformed log names stay visible after dated logs

The log list is shared by screen refresh and deletion flows, so date parsing must be best-effort. Standard timestamp-named files sort newest first; files without that timestamp remain visible after them, ordered by modification time and then name, while directories are excluded.

## Share and export MIME types share one extension mapping

CSV is `text/csv`; S3L and unknown extensions use `application/octet-stream`. Share intents and each export destination must use the same mapping so uppercase extensions and unsupported file types do not acquire a misleading text MIME type.

## A failed custom-tree write attempts cleanup of only its newly created document

The document URI returned by `createDocument` is the only URI eligible for cleanup. If opening, copying, or closing the output fails, attempt to delete that URI and preserve the write failure as the primary exception, attaching cleanup failure as suppressed detail.

## Tree permission flags come from the picker result

Persist only the returned read/write flags; persistable and prefix flags are not accepted by `takePersistableUriPermission`. A failed grant leaves the saved export directory unchanged. When the result has no read/write flags, preserve the existing behavior of saving the URI without requesting a grant.
