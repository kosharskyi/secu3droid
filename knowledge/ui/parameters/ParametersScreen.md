## Connection status gates page controls before session cleanup finishes

When the connection is not `Connected`, render the connection status instead of page contents, hide the numeric editor and disable Save. The ViewModel independently clears packet holders and pending operations, but that coroutine cleanup must not be the only barrier against a queued UI action after disconnect. Reconnection reactivates only the settled, visible page; adjacent precomposed pages must not read the ECU.

## Pager motion does not end the settled page's read session

Keep the active page activation while a horizontal pager gesture is in progress, including boundary overscroll or a canceled swipe. A vertical child scroll, overscroll, or nested fling can also toggle pager `isScrollInProgress` without changing its settled page, so scrolling is not an activation boundary on either axis. Manual-command predicates still become false during movement. Once the pager settles, activate only if its settled index changed; connection loss, lifecycle pause, and connection-session replacement still invalidate the session immediately.

## Newly offscreen parameter forms wait until pager idle

Keep the currently visible pages and the full `currentPage..targetPage` range composed immediately. Defer only a page that is first entering the beyond-viewport area during motion, then latch it rendered after its connected form content has actually been composed. This moves expensive form construction off the drag path while preserving adjacent-page continuity and immediate rendering during long tab jumps; retain a full-size empty slot so pager geometry does not change.
