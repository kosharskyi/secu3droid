## Connection and discovery use different Bluetooth permission sets

The connection request and resuming device discovery intentionally have separate permission requirements. On Android 12 and later they currently request/check `BLUETOOTH_CONNECT` and `BLUETOOTH_SCAN`; on older Android, connection also includes coarse location while discovery includes `BLUETOOTH_ADMIN`. Keep the operation-specific sets separate when changing platform behavior.

## The API 31 legacy permission exception is limited to Xiaomi and POCO devices

Connection requests include legacy `BLUETOOTH` on API 31 only when the manufacturer or brand is Xiaomi, the brand is POCO, or the model contains POCO. Android 12's adjacent releases do not receive this exception; matching uses case-insensitive device strings.
