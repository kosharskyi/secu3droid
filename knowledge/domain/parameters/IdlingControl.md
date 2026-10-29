## P/I signs follow the UI labels, while wire indexes run negative then positive

The firmware `IDLREG` arrays use index 0 for the negative error and index 1 for the positive error. The existing Android resource-backed rows keep `P0/I0` for positive and `P1/I1` for negative, so ViewModel routing must map those legacy tags explicitly to the named positive/negative control commands. IFAC1/IFAC2 are already positive/negative and keep their existing field order. Do not swap the packet codec or rewrite received ECU values.

## THRASSMAP substitutes the throttle-assist table for IAC_ADD

The IAC_ADD editor is available only when fuel injection is supported and the ECU is not using the throttle-assist table. Recheck this current packet condition when the command is admitted so an editor callback opened before the flag changed cannot send a stale value.

## Unrelated IDLREG edits preserve raw fields and flag bits

Incoming values outside editor bounds remain visible and intact until that field is explicitly edited. Refused sends restore all known values and the complete flag byte on the original packet; successful sends use a copied packed snapshot. The reserved high flag bit and packet tail must survive edits.
