## Wire rows increase with load, and the highest row appears at the top

Each correction index is `loadIndex * 16 + rpmIndex`. Keep selection indices in wire order, and reverse the rows for display and SECU-3 Manager exports. The domain returns fractional indices in wire order, so the marker inverts its vertical coordinate and interpolates between cell centers rather than snapping to a cell. The frozen axis labels share the data grid's corresponding scroll position.

## EEPROM commands apply to both tables

Both EEPROM commands affect both tables. Dismiss an open reset confirmation when the route pauses or loses its connection because it belongs to the current ECU session. A retry after a reset readback failure invokes the regular map refresh.

## Keep the CSV snapshot outside the ECU session while the document picker is open

Opening the system document picker can pause the route and clear its ECU maps. Serialize the selected table and keep that snapshot in saveable launcher state before opening the picker. The result consumes the pending snapshot once, including on cancellation.

## VE correction uses the two LTFT banks captured before opening a BIN

The open picker can pause the ECU route and returning can trigger a fresh read. Keep the captured banks and original BIN in the route's ViewModel across picker pauses and Activity recreation. Canceling Save As returns to the same set and correction source. A new process starts a new operation.
