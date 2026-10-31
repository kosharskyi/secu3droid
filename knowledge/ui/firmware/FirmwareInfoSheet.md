## Display the complete firmware tag

The tag is the fixed-width 52-byte signature in FWINFO and can contain build suffixes that distinguish firmware releases. The sheet already scrolls, so do not cap the tag's line count or ellipsize its trailing bytes.

## The firmware sheet preserves its capability subset

The sheet displays 27 supported flags in its existing order. The FWINFO options word also includes reserved and obsolete bits; do not add those to the display merely because they appear in the protocol model.

## Keep the sheet's width cap on the touch panel too

The firmware sheet is capped at 640dp and anchored to the bottom of the window. Its route-level empty click target must share that cap; if the target fills the window, taps beside the sheet are swallowed instead of dismissing the modal.
