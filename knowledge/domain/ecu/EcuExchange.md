## The response collector must be active before the ECU command is sent

Use `EcuExchange.request` for one-shot ECU replies. The manager invokes the request callback only after subscribing to the selected transport, preventing fast replies from racing collector startup. Its own deadline becomes `TimeoutException`; cancellation from a parent job remains cancellation.
