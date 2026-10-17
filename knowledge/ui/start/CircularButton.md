## The connection indicator needs two distinct colors

The circular button's track and progress arc must use different semantic colors; a single color makes active progress indistinguishable from the track. Both strokes need to be inset by half the widest stroke because the circle clip otherwise cuts off the progress edge.

## Size from both state labels before showing either one

The Start screen's 200 dp value is a minimum diameter. `CircularButton` measures both localized labels up front and uses one diameter for normal and connecting states, so the transition cannot move the circle. When the available safe slot cannot fit the full text at the normal type size, choose one fitted text size for both labels; keep the device font scale active, and do not ellipsize or break a state label across lines. Draw the ring from the actual Canvas bounds because a parent can constrain the measured diameter.

## Keep the circle inside the center slot

The Start layout reserves a top row and a bottom USB-hint slot around the weighted center slot. The reserved slots keep an enlarged button away from the controls and preserve the center when the USB hint is absent. Tests must use the actual call site and switch state after measuring bounds; a standalone preview does not exercise the same constraints.
