## Parse the full 52-byte signature before firmware options

The FWINFO wire payload is a fixed 52-byte signature followed by a four-byte options word and one packed version byte. Reading a shorter signature shifts the options/version boundary into signature text and can falsely enable or disable firmware-gated pages (including the SECU3T/Micro family gate). Keep these offsets aligned with the desktop `FW_SIGNATURE_INFO_SIZE` protocol definition.
