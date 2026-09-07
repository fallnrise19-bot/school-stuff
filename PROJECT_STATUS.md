# School Stuff project status

Current prototype: 0.1.15 / versionCode 16

This repository is configured to build a debug APK automatically on every push to `main` using GitHub Actions.

Current prototype areas:
- Today / Tomorrow school dashboard
- Multiple child profiles
- Recurring school routines
- Homework, forms and bring-item tracking
- Teacher and school information
- Papers and Memories document references
- Android notifications
- Android Calendar Provider import/write-back

Current test focus: verify child-profile scrolling on Samsung after build 15's report identified a legacy-null transportation field at `SchoolStuffApp.kt:615`. Build 16 repairs older saved children/items/documents on load and adds a second defensive null boundary in the Transportation UI. Subscription/cloud architecture remains next only after this crash is confirmed fixed on hardware.
