## ADC writes keep per-channel zero-factor raw fallback transactional

An explicit factor or correction edit marks its ADC channel as edited even if the selected Float is unchanged. If `ParameterSession` refuses the resulting packed command, restore the packet-owned checkpoint so a later accepted command cannot retire or canonicalize that channel's received correction word. Captured output bytes are immutable because packet admission may continue with later edits.

## ADC field gates distinguish TPIC8101 from SECU3T-only channels

Channels MAP through ADD_I2 are always editable. ADD_I4 follows TPIC8101 independently; ADD_I3 and ADD_I5 through ADD_I8 follow `dbw` (non-SECU3T). Unknown firmware is permissive for field editing, matching the parameter editor's existing preview behavior.
