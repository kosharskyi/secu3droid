## Prime-pulse conversion assumes ATmega1284

The supported ECU platform is ATmega1284, so both reading and writing prime pulses use a fixed 3.2-microsecond discrete. Do not select a different scale from deprecated MCU option bits or missing firmware information.

## After-start stroke values are encoded in four-stroke counts

Petrol and gas after-start values are retained as whole strokes, but wire bytes store counts of four strokes. Encoding divides by `4.0` and rounds half away from zero, so manually accepted values between the UI's four-stroke spin steps are rounded instead of truncated.
