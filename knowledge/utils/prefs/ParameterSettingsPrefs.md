## Fuel density settings publish as one successfully read pair

Primary and secondary injector densities live in the separate `parameter_preferences` DataStore. Until its first successful read, the shared nullable state remains unset so ECU-derived edits fail closed. Missing or invalid persisted values resolve independently to 0.710 and 0.536 g/cm³. Save writes the pair atomically and publishes the committed DataStore snapshot before returning; comma and dot decimal input both accept values from 0.2 through 2.0.
