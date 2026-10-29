## Fan hysteresis corrections depend on the edited field and PWM mode

The desktop uses a 0.25 °C gap: editing FAN ON lowers FAN OFF when necessary only while PWM is off; editing FAN OFF raises FAN ON when necessary even while PWM is on. Explicitly enabling or disabling PWM lowers FAN OFF when necessary. Loading and unrelated edits keep the received pair unchanged.

If an automatic correction would leave the editor range, Android rejects the full command and restores the packet in place. This is the approved boundary behavior, rather than reproducing the desktop's recursive editor notifications.

## Firmware gates are checked again when each command is sent

PWM mode and its frequency require the live PWM option, coolant curve selection requires the live thermistor-curve option, and conditioner pressure fields require both fuel injection and non-SECU-3T firmware. Conditioner minimum RPM requires fuel injection. Keep UI row enabled state as presentation; stale callbacks still reach these domain checks.

## The whole-second timer editor does not quantize unrelated packet writes

The ECU timer arrives as unsigned centiseconds in a Float. Keep that raw Float until the timer itself is explicitly changed to a whole number of seconds; a flag, threshold, or other numeric edit must retain its original hundredths.
