## Fan timer retains unsigned centiseconds beneath the whole-second UI

The ECU stores the unsigned timer in hundredths of a second. Keep the decoded Float in the packet so unrelated edits preserve centiseconds; round only when encoding or preparing the whole-second editor value.

## Fan-on and fan-off thresholds use signed quarter-degrees

The two fan temperature words are signed 16-bit values divided by four. Pack with protocol half-away-from-zero rounding so negative half-count edits do not round toward zero.
