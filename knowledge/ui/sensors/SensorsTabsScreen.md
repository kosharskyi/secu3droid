## Sensor pages and tabs use different horizontal window insets

The tab strip spans the width protected by horizontal system bars, while the pager page also receives horizontal safe-drawing insets for display cutouts. Consume the Scaffold padding before applying safe-drawing insets to the pager so only the cutout difference is added; otherwise landscape pages get double padding. Keep vertical safe-drawing insets on the Scaffold. The logger FAB slot adds only `safeDrawing.exclude(systemBars)` horizontally because Scaffold already positions it clear of navigation bars.

## Logging controls depend on visibility and whether a file is open

The visibility preference applies to Start, Stop, and all mark buttons. When controls are visible, users can stop a file that the manager started automatically or restart it with the same Start action. The screen only needs the logger's active state.
