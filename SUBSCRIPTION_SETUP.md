# ParentBell monthly subscription

Build 43 (0.1.23) keeps the subscription requirement enabled while letting unpaid users explore Home, Calendar and Kids with a clearly labelled example family. The preview is read-only and isolated from real school storage. Users can open example item details, browse calendar dates/months and open example child profiles; subscription buttons lead to the existing Play purchase page. Purchase and basic restart/restore passed in build 41. The owner reported expiry reset and blocked app access in build 42; build 43 addresses the lack of a useful preview. This exact candidate still needs device validation before production.

## Google Play setup

Upload a bundle containing Billing Library 8.3.0 to an internal or closed track using ParentBell's existing upload key. Keep the package `ca.creativepixels.schoolstuff`. This repository produces an unsigned release bundle when the upload signing environment variables are unavailable; it must be signed with the existing ParentBell upload key before Play accepts it. Do not generate a replacement key.

Create subscription `parentbell_monthly`, named ParentBell Monthly. Add the auto-renewing base plan `monthly`, with billing period one month and the Canadian price CAD $3.99. Select availability countries and review other currencies in Play Console. Activate the base plan. Do not add a trial, introductory offer, installments or a prepaid plan; the app intentionally selects only the regular monthly base plan.

ParentBell's Base64 PUBLIC licensing/billing key from Play Console is configured in `publicKey` in `billing.properties`. This public verification key can be committed to the repository. It is NOT the private upload keystore, keystore password or a service-account credential. It may also be provided as `PARENTBELL_BILLING_PUBLIC_KEY` during builds.

## Before enabling payment enforcement

Checkout and basic restart/restore were tested in build 41, so `requireSubscription=true` is now enabled for internal validation in build 42. A normal debug sideload is not proof that live billing works. Closed-test enrolment alone does not make somebody a licence tester; configure that separately to avoid real charges.

Verify the displayed CAD $3.99 monthly price, the Google Play checkout disclosure, purchase and automatic app unlock, repeated taps without a second checkout, cancelled checkout, pending payment without unlock, delayed completed payment on return, acknowledgement, restore with the same Play account after reinstall, purchase using a different Play account, cancellation with access through the paid period, expiry/refund revocation, interrupted network access, and English/French navigation. Confirm saved children and school items survive an update and subscription cancellation.

`requireSubscription=true` is enabled in build 42 with a higher versionCode. Build and test this exact configuration before promoting it to production. The build rejects missing or malformed public verification keys. An unpaid or expired subscription shows read-only examples on Home, Calendar and Kids. Real child/item subpages remain inaccessible. The preview has no real-data ViewModel, persistence, editing, calendar import or notification actions. Settings, language, security and subscription management remain available. Calendar import/write controls are hidden without access. Cancelling or expiry does not erase local data or cancel already-scheduled local reminders.

Review the Play listing/privacy policy to disclose Google Play payments and local school data. This subscription does not introduce cloud storage or household sharing. Completing closed testing and applying for production access is separate from activating subscriptions; do not publish this preparation build as the paid final release.

## Verification and offline behaviour

This local-only app verifies Google's signed purchase JSON with its public RSA billing key, checks the package/product and PURCHASED state, and acknowledges verified initial purchases through BillingClient. It queries current ownership at app resume and before checkout, ignores other products, and clears access on a successful query with no valid active purchase. Pending or invalid purchases never unlock the app or get acknowledged.

A signed receipt in Android's no-backup directory allows up to 24 hours of offline access after the last successful ownership query. A failed network request preserves that bounded access; an empty successful query revokes it immediately. Device-clock rollback invalidates the cache. Cancellation status comes from Play, not a locally invented expiry date. Cached receipts and purchase tokens are never committed to GitHub and do not contain children's school records.

This is client-side verification, not a secure subscription backend. Google recommends server-side token verification and acknowledgement via the Android Publisher API and real-time developer notifications. Client-side enforcement can be patched on a modified device, and the offline window can temporarily preserve access after a remote refund or expiry. There is no backend or service-account credential in this repository; moving verification to a backend is a separate integration, not something this build pretends to provide.

Official references:
- https://developer.android.com/google/play/billing/integrate
- https://developer.android.com/google/play/billing/security
- https://developer.android.com/google/play/billing/test
