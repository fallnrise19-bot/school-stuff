# School Stuff

School Stuff is a parent-first Android school organizer: Today/Tomorrow reminders, multiple children, homework and forms, teacher/school details, document references, notifications, and Android/Google Calendar integration through the phone's Calendar Provider.

## Current build
- Version: 0.1.13
- versionCode: 14
- Package: `ca.creativepixels.schoolstuff`
- Minimum Android: 8.0 (API 26)
- Target/compile SDK: 35

## Debug APK from GitHub
Every push to `main` runs `.github/workflows/build-debug-apk.yml` and uploads `SchoolStuff-debug-apk` as a GitHub Actions artifact. The workflow deliberately uses GitHub's installed Gradle 8.9 so the repository does not need to commit a Gradle wrapper JAR.

## Included in the current build
- School-themed Home dashboard with Today/Tomorrow
- Oliver, Logan and Chloe starter profiles (editable/removeable)
- Add School Thing form with category/date/repeat/reminder/notes
- Recurring weekly school items
- Homework / forms / bring-item tracker
- Teacher & school information per child
- Papers & Memories references using Android's document picker
- Calendar screen
- Android Calendar Provider connection, calendar selection, import and write-back
- Local notifications through WorkManager
- Local on-device storage (SharedPreferences JSON) so there is no server/account requirement yet
- App-level internal navigation history so Android Back returns one screen at a time
- Safe-gesture window insets around scrollable content on gesture-navigation devices

## Calendar design
School Stuff uses Android's `CalendarContract` provider. This lets it work with Google calendars already synced to the device without shipping a Google API key. Users explicitly grant calendar permissions and choose the calendars School Stuff may read. They also choose one destination calendar for School Stuff write-back.

## Not included yet
- Cloud sync / family sharing
- Google Play subscription billing
- AI/photo extraction of school notices
- True file backup (0.1.0 stores persisted document URI references)
- Production signing / Play Store AAB

Those are intentionally later so the first installable app stays small and testable.
