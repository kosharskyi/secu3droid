## Received CKPS values remain raw until an explicit edit

The control preserves received selector codes and signed fractional Hall angles when it publishes values. Teeth-before-TDC choices are recalculated from wheel/cylinder geometry, but a now-excluded received value is retained until a valid explicit choice is made.

Hall angle bounds are editor metadata only; the control retains the existing finite signed/fractional command API instead of narrowing it to the editor's positive ranges.

## Missing-teeth zero keeps the current MTTF factor

Only explicit integer missing-teeth edits from 0 through 6 are admitted. Positive values select the firmware's legacy MTTF lookup entry; zero leaves the existing factor untouched. This prevents an invalid editor value from indexing the table and preserves the ECU factor when disabling missing teeth.

## Each CKPS command checks current firmware capabilities

The control reads the manager's current firmware flow when admitting a command. Keep CKPS, ignition-cog, Hall-window, MTTF, rising-spark, merge-output, cam-reference, TDC-selection, and cylinder-selection rules aligned with `ParameterCapabilities`; selector callbacks can outlive the presentation snapshot that enabled them.

## Outgoing CKPS commands capture packed bytes

`CkpsParamPacket` is mutable and parameter sends are asynchronous. Snapshot its packed bytes at admission so a later edit cannot alter an already queued command; packet packing still preserves unknown Hall flag bits and trailing bytes.
