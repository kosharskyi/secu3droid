## LTFT field commands require fuel injection even when the mode is Off

The desktop enables the complete LTFT page from the FI option, independently of the selected Off/Petrol/Gas/Petrol + Gas mode. Keep every numeric edit and mode selection behind the live FI field capability, while null firmware remains permissive for field admission. Do not use the LTFT page-visibility predicate for command admission because it intentionally rejects unknown firmware.

## LTFT receive preserves values outside editor bounds until that field is edited

Temperature fractions, raw values outside desktop editor limits, unknown mode codes, and the packet's unhandled tail must survive unrelated writes. Desktop performs no paired-threshold correction; the domain validates only the explicitly submitted field, leaving unrelated received values intact. The packet codec owns its existing signedness and fixed-point conversions; editor limits are not packet-wide validation rules.
