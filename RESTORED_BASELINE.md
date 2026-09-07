# Restored stable baseline

The active application source is restored from the known-good School Stuff 0.1.6 / build 7 snapshot:

- Original source commit: `6d07f922e9d07bd6994ed3f3c7b446db564374df`
- Matching Google Drive package: `SchoolStuff-0.1.6-build7.zip`
- Base runtime/UI source: build 7
- Android versionCode: 26 so this APK installs over earlier test builds without uninstalling or clearing local app data
- Display version: `0.1.6-item-controls`
- The post-restore Papers & Memories flow remains minimal: choose a category, pick a file, and open it with Android's normal image/PDF viewer. There is no custom gallery or in-app image decoding.
- Transportation is stored separately from existing child records to avoid legacy null-field crashes. Parent Notes uses its own simple text setting.
- The Add School Thing screen restores the real calendar picker, category dropdown with a Custom option, and optional emoji picker. The emoji field is nullable so older saved items remain safe.
- Settings adds notification permission status, adjustable reminder times, a default reminder for new items, phone notification settings, and a test notification.
- Settings groups are compact accordions so only one group of controls is expanded at a time.
- Security settings can require Android fingerprint, face, PIN, pattern, or password authentication at launch and after 30 seconds in the background.

Later feature work and crash-fix attempts remain recoverable in Git history, but are intentionally not part of this active baseline. Reintroduce later features individually only after real-device verification.
