#!/usr/bin/env bash
set -uo pipefail
gradle :app:connectedOptimizedTestAndroidTest --stacktrace
smoke_result=$?
mkdir -p app/build/reports/androidTests/screenshots
adb pull /sdcard/Download/. app/build/reports/androidTests/screenshots/ || true
exit "$smoke_result"
