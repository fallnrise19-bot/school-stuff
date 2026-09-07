# Restored stable baseline

The active application source is restored from the known-good School Stuff 0.1.6 / build 7 snapshot:

- Original source commit: `6d07f922e9d07bd6994ed3f3c7b446db564374df`
- Matching Google Drive package: `SchoolStuff-0.1.6-build7.zip`
- Base runtime/UI source: build 7
- Android versionCode: 19 so this APK installs over later test builds without uninstalling or clearing local app data
- Display version: `0.1.6-transport-notes`
- The only post-restore feature is a minimal Papers & Memories file flow: choose a category, pick a file, and open it with Android's normal image/PDF viewer. There is no custom gallery or in-app image decoding.
- Transportation is stored separately from existing child records to avoid legacy null-field crashes. Parent Notes uses its own simple text setting.

Later feature work and crash-fix attempts remain recoverable in Git history, but are intentionally not part of this active baseline. Reintroduce later features individually only after real-device verification.
