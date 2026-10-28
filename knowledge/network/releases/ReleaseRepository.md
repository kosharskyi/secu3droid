## Successful release responses consume the day's check

The daily timestamp is written after a successful GitHub response but before parsing or comparing its version. A malformed or older release therefore still counts as checked, while a request failure does not. Keep the timestamp write outside the network failure handling so DataStore I/O errors reach Home's existing storage error message. The entire production date-check and request sequence is serialized because Home can be entered repeatedly while a request is in flight.
