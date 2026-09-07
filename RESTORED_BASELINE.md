# Restored stable baseline

The active application source is restored from the known-good School Stuff 0.1.6 / build 7 snapshot:

- Original source commit: `6d07f922e9d07bd6994ed3f3c7b446db564374df`
- Matching Google Drive package: `SchoolStuff-0.1.6-build7.zip`
- Runtime/UI source: unchanged from build 7
- Android versionCode: raised from 7 to 17 only so this APK can install over later test builds without uninstalling or clearing local app data
- Display version: `0.1.6-restored`

Later feature work and crash-fix attempts remain recoverable in Git history, but are intentionally not part of this active baseline. Reintroduce later features individually only after real-device verification.
