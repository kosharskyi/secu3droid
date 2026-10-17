## ADC correction inversion depends on the packet mode

The raw correction word represents `(0.5 + correction/0.0025 * divisor) * 16384`, with divisor equal to the channel factor only in multiply mode and 1 otherwise. Dividing by the factor in disabled mode changes unrelated channels on the next whole-packet write.

## Undefined zero-factor corrections retain their incoming words

In multiply mode a zero factor has no inverse correction. Display finite zero and preserve the incoming word until that correction is explicitly edited or the factor/mode permits canonical encoding. Keep the received packet instance: a data-class copy loses the private raw fallback. UI correction mutations must use `setAdcValue`, which marks the channel edited; flags and extension bytes must survive unrelated edits.
