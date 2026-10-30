## Threshold confirmations are bound to the condition shown when editing began

Each condition slot has its own encoding. The caller passes the condition ID captured when the dialog opens; reject the confirmation if the live condition changed, the output is disabled, or slot two is now NONE. Selecting a condition applies that catalog entry's ON and OFF defaults to only that slot, including when the displayed number matches the prior value but the wire scale differs.

## Output configuration requires the current output to be enabled

Enabling or disabling an output is always allowed. All other per-output edits require that output to be enabled, and slot-two edits also require a non-NONE logic function. Pair logic changes require outputs 1 and 2 enabled. These are desktop editor gates; firmware may still process a physically routed paired output independently of its use flag.

## Unknown packet words survive rejected and unrelated edits

A failed pack or `ParameterSession` admission restores the packet's raw fields in place. Preserve unknown condition IDs, raw threshold words, flag bits, pair code, and the packet's unhandled tail rather than reconstructing the packet from displayed floats.

## ADD_I2 threshold editors stop at 5 V while legacy wire values remain raw

Condition 16 (`ADD_I2`) uses a 5 V upper bound in the editor, with its existing 0.1 V step, two decimal places, and 2.5/2.48 V defaults. This deliberately differs from Manager's 600 V limit. The packet uses 400 counts per volt: 5 V encodes as 2000 (`07D0`), while 163.84 V requires 65536 counts and wraps to zero in the unsigned 16-bit word. Do not normalize an incoming value above the editor range during receive or an unrelated edit; only explicit threshold commands are range-checked.
