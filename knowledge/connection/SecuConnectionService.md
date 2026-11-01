## Service destruction has no authority over the connection

The service only maintains its foreground notification and performs local cleanup. It does not inject the connection manager or report destruction to it. Connection statuses and the background preference determine service commands in `Secu3ConnectionManager`; stopping or replacing an Android service instance must never stop a transport through a reverse callback.
