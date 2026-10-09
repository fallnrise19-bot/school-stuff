# ParentBell project status

Current build: 0.1.24 / versionCode 44

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

Subscription page is now available from Settings in English and French, with Google Play product pricing, checkout, restore, status and management/cancellation links. Whole-app subscription enforcement is ON for internal validation in build 42. The basic Play licence-test purchase and restart/restore flow passed in build 41; the lock and remaining launch checks still need device testing before production. See SUBSCRIPTION_SETUP.md for product configuration, verification key and launch checks. Cloud backup and family sharing are not included or advertised.

Build 40 removes Coming soon and testing-access text from the subscription page and Settings summary. Missing subscription information uses normal status/error messages; purchase verification and enforcement configuration are unchanged.

Build 41 configures ParentBell's public Google Play billing key for purchase verification. It is intended for an internal or closed test track, with the `parentbell_monthly` subscription and `monthly` base plan at CAD $3.99/month. Enforcement remains off until Play-installed purchase/restore checks pass. The final paid production build must use a higher versionCode and enable enforcement after those checks.

Build 42 enables the existing subscription gate for Home, Calendar, Kids and child/item subpages. Subscription management, language, security and About remain accessible in Settings. This is an internal-validation candidate, not a production approval. No local school-data schema, migration or deletion changes were made.

Build 43 replaces the unpaid Home/Calendar/Kids paywall with an interactive, read-only example family preview. Users can filter example school items, open their details, browse calendar months/dates and open example child profiles. Clearly labelled examples use the app artwork and English/French copy. The preview never receives the real data ViewModel or writes to school storage. Subscription remains required for real family information and changes. Subscription and Settings remain accessible; subscribers continue to use the existing real screens. Device validation is still required before production.

Build 44 enables R8 code optimization and resource shrinking for release builds. Targeted rules preserve the six local JSON models, the signed receipt cache, and the existing reminder worker name so stored school data and queued reminders remain compatible. No storage migration or subscription policy changes are made. Edge-to-edge setup uses AndroidX with safe-drawing insets and inset consumption; the lock screen scrolls in short windows. Portrait-only orientation is removed and the activity is resizable. Compose handles window configuration changes without recreation so in-progress forms and navigation survive rotation/resizing. Content has a maximum reading width of 840dp on large windows. Automated builds and optimized-runtime smoke checks must pass; Play billing and physical-device layout checks remain required before production.
