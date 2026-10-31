## Reconnect attempts keep the selected device identity

Inside its cancellable preparation job, `BtConnection` reads and validates the selected MAC address. Every retry in that connection uses that address, even if preferences change while retries are underway; a new connection start reads the latest selection. Device names are display metadata and cannot resolve a transport target.
