## Knock control threshold is a signed quarter-degree word

The coolant threshold wire word is signed 16-bit and scaled by 0.25 °C per count. High-bit raw values represent negative temperatures; treating them as unsigned can turn a cold threshold into a very large positive value. Other knock words have their own wire conventions and are not covered by this invariant.
