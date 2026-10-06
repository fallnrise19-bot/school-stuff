# ParentBell project status

Current build: 0.1.20 / versionCode 40

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
- Confirmed item deletion from dashboard lists
- Monday-to-Friday date strip on child dashboards
- Papers and Memories references with categorized upload and Android file viewing
- Notification permission status, adjustable reminder times, a default reminder, and a test notification
- Compact accordion-style Settings cards so only one group of controls is open at a time
- Optional Android app lock using an enrolled fingerprint, face, or device credential, with a 30-second background grace period and hidden recent-app preview
- Android Calendar Provider import/write-back

Subscription page is now available from Settings in English and French, with Google Play product pricing, checkout, restore, status and management/cancellation links. Whole-app subscription enforcement is implemented but OFF in this preparation build so testers are not locked out before Play setup is complete. See SUBSCRIPTION_SETUP.md for product configuration, verification key and launch checks. Cloud backup and family sharing are not included or advertised.

Build 40 removes Coming soon and testing-access text from the subscription page and Settings summary. Missing subscription information uses normal status/error messages; purchase verification and enforcement configuration are unchanged.
