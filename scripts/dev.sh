#!/usr/bin/env sh
# USB live loop: build, install and open the debug build on the connected phone.
set -e
cd "$(dirname "$0")/.."
./gradlew :app:installDebug
adb shell am start -n dev.intelia.launcher.dev/dev.intelia.launcher.MainActivity
