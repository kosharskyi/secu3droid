## Fan timer retains unsigned centiseconds beneath the whole-second UI

The ECU stores the unsigned timer in hundredths of a second. Keep the decoded Float in the packet so unrelated edits preserve centiseconds; round only when encoding or preparing the whole-second editor value.
