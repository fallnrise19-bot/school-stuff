# ParentBell monthly subscription

Build 42 (0.1.22) enables the whole-app subscription requirement for internal validation. On 8 October 2026, the owner completed a Play-installed licence-test purchase and reported Active status after restarting the app and tapping Restore purchases. This validates that basic flow, not the entire launch checklist. The exact enforcement build still needs testing for subscribed access, unpaid/expired blocking and the remaining scenarios below before production. Existing app data is preserved, and the app price comes from Google Play.

## Google Play setup

Upload a bundle containing Billing Library 8.3.0 to an internal or closed track using ParentBell's existing upload key. Keep the package `ca.creativepixels.schoolstuff`. This repository produces an unsigned release bundle when the upload signing environment variables are unavailable; it must be signed with the existing ParentBell upload key before Play accepts it. Do not generate a replacement key.

Create subscription `parentbell_monthly`, named ParentBell Monthly. Add the auto-renewing base plan `monthly`, with billing period one month and the Canadian price CAD $3.99. Select availability countries and review other currencies in Play Console. Activate the base plan. Do not add a trial, introductory offer, installments or a prepaid plan; the app intentionally selects only the regular monthly base plan.

ParentBell's Base64 PUBLIC licensing/billing key from Play Console is configured in `publicKey` in `billing.properties`. This public verification key can be committed to the repository. It is NOT the private upload keystore, keystore password or a service-account credential. It may also be provided as `PARENTBELL_BILLING_PUBLIC_KEY` during builds.

## Before enabling payment enforcement

Checkout and basic restart/restore were tested in build 41, so `requireSubscription=true` is now enabled for internal validation in build 42. A normal debug sideload is not proof that live billing works. Closed-test enrolment alone does not make somebody a licence tester; configure that separately to avoid real charges.

Verify the displayed CAD $3.99 monthly price, the Google Play checkout disclosure, purchase and automatic app unlock, repeated taps without a second checkout, cancelled checkout, pending payment without unlock, delayed completed payment on return, acknowledgement, restore with the same Play account after reinstall, purchase using a different Play account, cancellation with access through the paid period, expiry/refund revocation, interrupted network access, and English/French navigation. Confirm saved children and school items survive an update and subscription cancellation.

`requireSubscription=true` is enabled in build 42 with a higher versionCode. Build and test this exact configuration before promoting it to production. The build rejects missing or malformed public verification keys. An unpaid or expired subscription routes Home, Calendar, Kids and child/item subpages to the subscription page. Settings, language, security and subscription management remain available. Calendar import/write controls are hidden without access. Cancelling or expiry does not erase local data or cancel already-scheduled local reminders.

Review the Play listing/privacy policy to disclose Google Play payments and local school data. This subscription does not introduce cloud storage or household sharing. Completing closed testing and applying for production access is separate from activating subscriptions; do not publish this preparation build as the paid final release.

## Verification and offline behaviour

This local-only app verifies Google's signed purchase JSON with its public RSA billing key, checks the package/product and PURCHASED state, and acknowledges verified initial purchases through BillingClient. It queries current ownership at app resume and before checkout, ignores other products, and clears access on a successful query with no valid active purchase. Pending or invalid purchases never unlock the app or get acknowledged.

A signed receipt in Android's no-backup directory allows up to 24 hours of offline access after the last successful ownership query. A failed network request preserves that bounded access; an empty successful query revokes it immediately. Device-clock rollback invalidates the cache. Cancellation status comes from Play, not a locally invented expiry date. Cached receipts and purchase tokens are never committed to GitHub and do not contain children's school records.

This is client-side verification, not a secure subscription backend. Google recommends server-side token verification and acknowledgement via the Android Publisher API and real-time developer notifications. Client-side enforcement can be patched on a modified device, and the offline window can temporarily preserve access after a remote refund or expiry. There is no backend or service-account credential in this repository; moving verification to a backend is a separate integration, not something this build pretends to provide.

Official references:
- https://developer.android.com/google/play/billing/integrate
- https://developer.android.com/google/play/billing/security
- https://developer.android.com/google/play/billing/test
