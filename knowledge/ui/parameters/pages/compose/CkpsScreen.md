## CKPS selector options are supplied by the domain control

The screen renders the control's current teeth-before-TDC choices and preserves the raw displayed ECU value when geometry excludes it. Cylinder fallback text recognizes raw protocol IDs 1–8 even when current firmware filters one out of the dialog choices.

Odd cylinders require phased ignition, Hall sync, or merged outputs; seven cylinders additionally require ODDFIRE_ALGO. The existing domain capability snapshot owns this firmware-dependent filtering; the screen passes current merged-output state.
