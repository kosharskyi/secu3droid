## Knock option arrays are ordered wire-code labels

The resource arrays have 64 BPF labels and 32 integration timeout labels; each array position is the corresponding wire code. Keep labels in UI resources, display an incoming unknown code as `Code n`, and pass only an explicit selected code to the domain control. Canceling the shared choice dialog sends nothing.

## Knock channel checkboxes select a knock sensor per cylinder

Checkbox 1..8 changes the corresponding `selectedChanels` bit, which chooses first versus second knock sensor for that cylinder. Preserve the established row order, tags, and enabled switch presentation; this mask does not enable or disable cylinders.
