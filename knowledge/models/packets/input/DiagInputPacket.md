## DIAGINP `temperature` is an unsigned input voltage

The field named `temperature` is the voltage measured at the temperature sensor input, not a Celsius reading. Its raw 16-bit word is unsigned and scaled by 1/400 V; unlike converted sensor temperatures in other packets, values with bit 15 set remain positive.
