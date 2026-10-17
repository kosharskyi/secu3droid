## CKPS selector options depend on the wheel and ignition mode

Teeth-before-TDC bounds follow the manager's wheel/cylinder geometry (66-degree lower bound, upper bound 540/330/270 degrees depending on cylinder count). Keep the byte limit 255 even if the manager's computed range exceeds it. Do not replace a received value merely because the new geometry excludes it. Unknown geometry yields no choices instead of inventing a default.

Odd cylinders require phased ignition, Hall sync, or merged outputs; seven cylinders additionally require ODDFIRE_ALGO. Filtering the visible choices must preserve the raw displayed ECU value. The packet mutation accepts protocol IDs; the firmware filter belongs to the screen.
