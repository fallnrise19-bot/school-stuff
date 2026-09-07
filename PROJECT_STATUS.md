# School Stuff project status

Current prototype: 0.1.6-app-lock / versionCode 23

This repository is configured to build a debug APK automatically on every push to `main` using GitHub Actions.

Current prototype areas:
- Today / Tomorrow school dashboard
- Multiple child profiles
- Recurring school routines
- Homework, forms and bring-item tracking
- Teacher and school information
- Per-child bus or private transportation information
- Compact Parent Notes notebook on Home
- Calendar date picker, category dropdown with Custom, and optional item emoji
- Papers and Memories references with categorized upload and Android file viewing
- Notification permission status, adjustable reminder times, a default reminder, and a test notification
- Compact accordion-style Settings cards so only one group of controls is open at a time
- Optional Android app lock using an enrolled fingerprint, face, or device credential, with a 30-second background grace period and hidden recent-app preview
- Android Calendar Provider import/write-back

Next after the first successful APK install: phone UI testing, interaction fixes, then subscription/cloud architecture only after the local core is solid.
