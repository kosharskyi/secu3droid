## Five IDLREG correction fields are signed fixed-point values

`iFac1` and `iFac2` use signed 1/256 counts, the minimum/maximum regulator angles use signed 1/32-degree counts, and turn-on temperature uses signed quarter-degree counts. Preserve negative fractional values and use protocol half-away-from-zero rounding when encoding these words.

## RPM additions and integrator limit round to ten-RPM wire counts

Both UI values are integer RPM while the ECU stores unsigned counts of ten RPM. Typed values are range-checked but are not quantized to the displayed step, so encode nonmultiples of ten with protocol half-away-from-zero rounding rather than truncation.


## The P/I words use negative wire index 0 and positive index 1

The wire order is `P0 negative`, `P1 positive`, `I0 negative`, `I1 positive`, confirmed against firmware `secu3app` commit `442605eaac1241d0e764951e2dc732df03a987e4` and desktop `IdlRegPageDlg.cpp`. Android UI rows retain their resource labels (`P0/I0` positive, `P1/I1` negative) and map explicitly in `IdlingControl`/`ParamsViewModel`; keep `pack` and `parse` field order unchanged.
