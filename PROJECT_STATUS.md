# School Stuff project status

Current prototype: 0.1.14 / versionCode 15

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

Current test focus: verify child-profile scrolling on Samsung after isolating the gallery from the profile and replacing live Coil previews with bounded software bitmap decoding. If the device still crashes, reopen the debug app and copy the captured stack trace from the diagnostic dialog. Subscription/cloud architecture remains next only after this crash is confirmed fixed on hardware.
