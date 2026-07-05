#!/bin/bash

# Specify the file name
FILE_NAME="keystore.properties"

if [ -f "$FILE_NAME" ]; then
    echo "$FILE_NAME already exists. Skipping creation."
else
    echo "Creating $FILE_NAME..."
    cat <<EOF > "$FILE_NAME"
KEY_ALIAS=debug
KEY_PASSWORD=debug
KEYSTORE_PASSWORD=debug
KEYSTORE_PATH=debug.jks
EOF
    echo "Setup finished. Please ensure you have a 'debug.jks' file in the root directory."
fi
