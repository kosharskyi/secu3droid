## ADC correction inversion depends on the packet mode

The raw correction word represents `(0.5 + correction/0.0025 * divisor) * 16384`, with divisor equal to the channel factor only in multiply mode and 1 otherwise. Dividing by the factor in disabled mode changes unrelated channels on the next whole-packet write.

## Undefined zero-factor corrections retain their incoming words

In multiply mode a zero factor has no inverse correction. Display finite zero and preserve the incoming word until that correction is explicitly edited or the factor/mode permits canonical encoding. Keep the received packet instance: a data-class copy loses the private raw fallback. Domain edits must mark the channel edited, including explicit same-value writes; flags and extension bytes must survive unrelated edits.

## Refused ADC edits restore the packet's private raw-word history

`pack()` can retire zero-factor correction fallback by observing factor changes, and mode changes invalidate the parsed mode marker. ADC edit admission therefore checkpoints and restores the packet itself, including its parsed mode, raw factor/correction words, edited-channel flags, public payload fields, and protocol flags. Restoring only visible fields would allow a later unrelated accepted edit to rewrite previously received raw words.
