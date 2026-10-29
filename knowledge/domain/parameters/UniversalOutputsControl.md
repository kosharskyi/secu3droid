## Threshold confirmations are bound to the condition shown when editing began

Each condition slot has its own encoding. The caller passes the condition ID captured when the dialog opens; reject the confirmation if the live condition changed, the output is disabled, or slot two is now NONE. Selecting a condition applies that catalog entry's ON and OFF defaults to only that slot, including when the displayed number matches the prior value but the wire scale differs.

## Output configuration requires the current output to be enabled

Enabling or disabling an output is always allowed. All other per-output edits require that output to be enabled, and slot-two edits also require a non-NONE logic function. Pair logic changes require outputs 1 and 2 enabled. These are desktop editor gates; firmware may still process a physically routed paired output independently of its use flag.

## Unknown packet words survive rejected and unrelated edits

A failed pack or `ParameterSession` admission restores the packet's raw fields in place. Preserve unknown condition IDs, raw threshold words, flag bits, pair code, and the packet's unhandled tail rather than reconstructing the packet from displayed floats.
