#!/bin/sh
cd "$(dirname "$0")" || exit 1
./gradlew assembleDebug || exit 1
printf '\nAPK ready at app/build/outputs/apk/debug/app-debug.apk\n'
