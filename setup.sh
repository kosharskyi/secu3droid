#!/bin/bash

# Specify the file name
file_name="keystore.properties"

# Create the file and add four lines
echo "KEY_ALIAS=debug" > "$file_name"
echo "KEY_PASSWORD=debug" >> "$file_name"
echo "KEYSTORE_PASSWORD=debug" >> "$file_name"
echo "KEYSTORE_PATH=debug.jks" >> "$file_name"

echo "Setup finished"
