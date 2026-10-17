## Visual tab selection must not activate ECU pages

Parameters, Sensors, and Diagnostics pass the pager's `currentPage` for tab semantics and `currentPage + currentPageOffsetFraction` for the pill blend. Using `settledPage` for the visual selection delays feedback until the swipe finishes.

Keep parameter page activation and sensor task callbacks tied to settled pages. Parameters also require a resumed host, a laid-out visible page, and a connection; transient drag, reversal, overscroll, or nested fling without a settled-page change must not deactivate the active read session. Visual movement must not start reads or enable editing on a transitional page.

## Read fractional pager progress inside the tab component

Pass `selectionPosition` as a lambda and read it only inside each pill's `derivedStateOf`. Fractional progress changes each animation frame, but only the two adjacent pill fractions change; reading the progress in `SecuTabRow` or its tab-list scope would rerun the full tab-list composition and title traversal. The blend follows pager progress directly, so adding another timed animation would make it trail the gesture.
