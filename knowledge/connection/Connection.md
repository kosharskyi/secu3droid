## Stop cancels transport work but keeps cleanup and the outbound FIFO alive

Retry and read coroutines are children of the reusable connection scope and are cancelled on stop. Transport cleanup notifications and the serial write queue use a background scope that survives stop/start; cancelling it would lose a pending `Disconnected` event and permanently kill writes for a later reconnect. Stop detaches the endpoint immediately, then closes it and publishes its state from the background scope.
