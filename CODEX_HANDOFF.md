# SCHOOL STUFF — CODEX CONTINUATION HANDOFF

Read this before changing anything. Continue the existing Android project; do not restart or redesign it from scratch.

## Source of truth

- GitHub repo: `fallnrise19-bot/school-stuff`
- Android package: `ca.creativepixels.schoolstuff`
- Current app version: **0.1.13 / versionCode 14**
- The Google Drive folder is **School Stuff**.
- A current source ZIP is being placed in that Drive folder for Codex as well.

## CRITICAL CURRENT BUG — DEVICE VERIFICATION REQUIRED

The child-profile navigation/gesture problem was reworked in 0.1.13 and is **not considered closed until the user verifies it on the Samsung device**.

User test on 0.1.12: while inside a child's profile, swiping/scrolling with a sideways component still caused the app to leave/close/minimize to the Android launcher/home screen.

A `BackHandler(enabled = addingThing || childPage != null)` was added in 0.1.12 so system Back should return from Add Thing or Child Profile to the app rather than finish the Activity. The user explicitly confirmed that attempt did not solve the actual problem.

0.1.13 removes that local-state/BackHandler navigation architecture. `SchoolStuffNavigator` now owns a real internal history outside the composable tree, and `MainActivity` registers an `OnBackPressedCallback` before composing the UI. The callback is enabled synchronously whenever the internal back stack is non-empty, so a genuine system Back gesture pops exactly one School Stuff destination. The root Scaffold also uses `WindowInsets.safeContent`, keeping gesture-driven content outside Android's back/home gesture insets on edge-to-edge Samsung/Android devices. Predictive back is explicitly enabled on `MainActivity`.

Unit tests cover Main → Child → Add Thing → Child → Main back-stack order and callback availability. GitHub CI now runs these tests before assembling the debug APK.

The next verification must test both cases separately: diagonal/sideways scrolling within a child profile must not leave School Stuff, and a deliberate Android Back gesture must return exactly one level. If 0.1.13 still reaches the launcher during an ordinary scroll, establish whether Samsung is invoking the mandatory Home gesture from the bottom inset before changing navigation again; Android does not allow apps to intercept that gesture.

Do not re-add another guessed local `BackHandler` or `onResume` patch.

## LOCKED WORKING CALENDAR FIX — DO NOT BREAK

Calendar loading is currently confirmed working. This took several iterations. Preserve this behavior.

The successful pattern is:

1. Calendar permission is observable Compose state.
2. Permission callback immediately refreshes calendars.
3. Refresh retries briefly because Android Calendar Provider can lag.
4. Settings refreshes while open.
5. A `ContentObserver` watches `CalendarContract.Calendars.CONTENT_URI`.
6. Calendar Provider is queried directly for visible Google calendars (`ACCOUNT_TYPE == "com.google"`).

The user confirmed this finally loads email/calendar accounts immediately without leaving Settings and returning. Treat this as a locked regression-sensitive area.

## CURRENT FEATURES / STATE

### Core app
- Parent-focused school organizer.
- Multiple child profiles.
- Today/Tomorrow reminders.
- Add School Thing flow.
- Calendar tab plus Google/Android Calendar integration.
- Kids tab and child profile pages.
- Settings.

### Add School Thing
- Proper calendar date picker replaced the old +/- day controls.
- Category is a dropdown rather than a horizontal fixed chip list.
- Supports Custom category.
- Optional emoji selector for an item.
- Existing built-in categories still map to app artwork when no emoji is chosen.

### Child profiles
- Teacher and school information.
- Homework & Forms.
- Papers & Memories.
- Transportation section added with options such as School Bus, Parent/Caregiver, Walk, Other.
- Transportation fields can include bus/route number, driver/caregiver name, licence plate/vehicle info, pickup details, drop-off details, and notes.

### Papers & Memories / gallery
- User wanted uploaded photos/files to be visible instead of random storage filenames.
- Current design uses a lightweight child-profile summary plus `Open Gallery` so images are not decoded merely by scrolling the profile.
- Gallery shows image thumbnails and allows tap-to-enlarge.
- Non-image files/PDFs use file tiles and should open externally.
- Thumbnail decode is bounded (around 320 px) and enlarged preview decode is bounded (around 1600 px) to avoid large-camera-photo memory crashes.
- Preserve this memory-conscious behavior unless replacing it with something demonstrably safer.

### Visual asset pack
Real generated PNG assets are embedded as Android drawables and used in the app. Do not revert to screenshot crops/base64 placeholders or generic Material icons everywhere.

Assets include:
- blue backpack
- school books
- pizza
- sub sandwich
- gym shoes
- spirit-day shirt
- camera
- art palette/artwork
- forms/clipboard
- folder/report-card/homework visual

The user specifically likes the cute illustrated prototype look and wants these visuals retained and expanded thoughtfully.

## VISUAL / UX DIRECTION

- App name: **School Stuff**.
- Playful, parent-friendly school organizer.
- Soft notebook/sticker/school-supply personality.
- Rounded cards, cheerful colors, useful illustrations.
- Avoid bare/generic utility-app appearance.
- Keep screens practical and readable rather than overdecorated.

## DEVELOPMENT WORKFLOW PREFERENCE

The user strongly prefers not to do Android Studio/CLI busywork. Keep GitHub as source of truth, use CI to build debug APKs, and hand back an installable APK after changes.

Use the existing stable debug signing setup so updates install over previous debug builds. Do not casually replace the signing key.

Always bump `versionCode` for a new installable test build and keep `versionName` visible/readable in the app from actual build metadata, not hard-coded stale UI text.

## NEXT PRODUCT WORK USER WANTS CODEX TO HELP WITH

After the child-profile swipe/close bug is fixed, the user wants to continue substantial development, especially:

1. **Shared parent/caregiver account**
   - Allow another parent/caregiver to share the same family/school data.
   - Think through invitation/linking, permissions, sync, conflict handling, child-level access if useful, and privacy.
   - Do not bolt this on as a fake local-only UI; it needs a real data/account architecture.

2. **Subscription system**
   - User wants to move toward a real subscription product.
   - Define free vs premium sensibly before implementing billing.
   - Candidate premium areas previously discussed include shared caregivers, cloud sync, calendar sync/enhancements, document parsing or advanced convenience features.
   - Use Google Play Billing correctly and design restore/entitlement handling rather than just adding a paywall screen.

3. **Cloud/backend architecture**
   - Shared accounts and subscriptions will likely require moving beyond purely local state.
   - Evaluate the current data model before choosing implementation. Preserve offline usefulness where possible.

4. Continue UX polish and practical parent features without turning the app into a full replacement calendar.

## IMPORTANT BEHAVIORAL NOTES FOR CODEX

- Make changes in the existing project rather than rebuilding completed work.
- Inspect the current source before patching.
- Keep working features intact, especially the calendar fix.
- Build after meaningful changes and inspect compiler/runtime implications instead of handing the user speculative code.
- For the swipe/close bug, 0.1.12 is known to have failed in real-device testing; 0.1.13 still requires real-device confirmation.

## CURRENT HANDOFF POINT

The immediate next task is:

**Have the user install and test 0.1.13 build 14 on the Android/Samsung phone, then record whether diagonal scrolling and a deliberate Back gesture behave separately as intended.**

Once that is stable, proceed with account sharing, subscription architecture, and the next requested features.
