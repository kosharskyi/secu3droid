@echo off
set FILE_NAME=keystore.properties

if exist %FILE_NAME% (
    echo %FILE_NAME% already exists. Skipping creation.
) else (
    echo Creating %FILE_NAME%...
    (
        echo KEY_ALIAS=debug
        echo KEY_PASSWORD=debug
        echo KEYSTORE_PASSWORD=debug
        echo KEYSTORE_PATH=debug.jks
    ) > %FILE_NAME%
    echo Setup finished. Please ensure you have a 'debug.jks' file in the root directory.
)
pause
