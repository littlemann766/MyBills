# My Bills 20.1.6

20.1.6 is the cleanup and security-foundation release. It intentionally keeps the existing My Bills features and local data format while reducing repository baggage and hardening the Android wrapper.

## Android identity and updates

- Application ID: `com.mybills.app`
- Permanent release signing alias: `mybills`
- Current version: `20.1.6` (`versionCode 148`)
- Existing My Bills app data remains in the same Android application sandbox and the web data/storage keys are unchanged.

## Required GitHub Actions secrets

- `MYBILLS_KEYSTORE_HEX`
- `MYBILLS_KEYSTORE_PASSWORD`

Do not commit the keystore, its HEX representation, or its password to this repository.

## Security foundation in 20.1.6

- Android cleartext network traffic is disabled.
- The Android app no longer requests the INTERNET permission because the production wrapper uses bundled local assets.
- Android automatic app-data backup is disabled; My Bills uses its explicit user-controlled backup/restore feature instead.
- WebView debugging is explicitly disabled.
- File-URL cross-origin access and universal file access are disabled.
- Mixed HTTP/HTTPS content is blocked.
- Third-party cookies are disabled.
- External web pages cannot load inside the WebView that owns the Android JavaScript bridge; web links are handed to the device browser.
- A restrictive Content Security Policy blocks external scripts, frames, objects, and unexpected network connections while allowing the app's existing inline code.

## Cleanup in 20.1.6

- The active `MainActivity.java` now lives in the conventional `com/mybills/app` source path.
- Old placeholder Java source files and the unused `app/src/index.html` stub were removed.
- Obsolete v121-v125 PWA icon generations were removed from Android assets.
- Root and Android `index.html` are synchronized.
- PWA icon references now consistently use the current v126 artwork.
- Service-worker, README, upload notes, and build metadata are aligned to 20.1.6.

## Next production step

After this cleanup release is installed and tested, move the Android build to API 36+, add an Android App Bundle (`.aab`) release artifact, and begin store-readiness testing. The large legacy single-file UI should be refactored feature-by-feature only after a verified backup and regression baseline, rather than by deleting patch layers in one risky pass.


## Android 16 / Play Store foundation in 20.1.6

- Android compiles and targets API 36.
- Android Gradle Plugin is 8.10.1 and CI uses Gradle 8.11.1.
- Release automation produces both a signed APK and signed Play Store `.aab`.
- Draft privacy policy, Play Store listing, and release checklist are included.


## JavaScript cleanup phase 1 — 20.1.6

- Consolidated bottom navigation and one-page shell control into the v119 app shell.
- Removed the superseded v139 shell patch and its duplicate navigation listeners/timers.
- Replaced v137 per-tab refresh handlers with one `mybills:viewchange` event.
- Removed the already-disabled v69 legacy net block from production HTML.
- No localStorage keys, backup format, bill/payment data, spending data, or finance schema changed.


## JavaScript cleanup phase 2 — 20.1.6

- Starts the 20.1 release line requested for the next cleanup milestone.
- Keeps the consolidated event-driven navigation introduced in the previous stable build.
- Removes stale observer-cleanup scaffolding/comments left by earlier patch generations.
- Preserves the remaining active observers because they still participate in live UI synchronization; they will be removed only after their responsibilities are migrated safely.
- No localStorage keys, backup format, bill/payment data, spending/refund data, or finance calculation schema changed.


## First-run setup — 20.1.6

- New installations get a guided “Let’s get you set up” wizard.
- Setup captures hourly wage, pay frequency, payday, typical check amount, and the starting monthly amount used by Available Now.
- Setup can immediately open the full Add Bill form for the first bill.
- Existing users with finance data are automatically treated as already set up and are not forced through onboarding.
- App Lock education now clearly explains that My Bills uses the phone’s existing PIN/password/pattern or fingerprint; My Bills does not create or store a separate PIN.
- A small security reminder is shown for the first three Android launches and can be dismissed permanently.


## Accessibility / popup mobility — 20.1.6

- First-run setup now uses nearly the full available screen height and scrolls inside the dialog.
- Setup action buttons stay reachable while moving through long forms.
- Native dialogs and common popup containers get safe maximum heights and vertical scrolling.
- Added 44–48 px minimum touch targets for primary controls.
- Added strong `:focus-visible` outlines for keyboard/switch-access users.
- Form controls keep at least 16 px text to avoid tiny text and accidental browser zoom.
- Added support for `prefers-reduced-motion`.
- Added safe-area padding for phones with gesture bars, cutouts, and system UI.
- No finance calculations, saved-data keys, backup format, or Android security settings changed.


## App Lock explanation — 20.1.6

- The first-run setup explains App Lock before the user can enable it.
- The wording is intentionally simple: there is no separate My Bills PIN.
- If Android asks for a PIN, the user enters the same PIN used to unlock the phone.
- Fingerprint can be used when it is already configured on the phone.
- The setup and Settings screens state that My Bills does not see or store the PIN or fingerprint.


## Usability fixes — 20.1.6

- Refund-only periods now show a positive `+$X net returned` amount instead of a negative `net spent` amount.
- Removed the four progress bars from first-run setup; setup now uses simple `Setup 1 of 4` text.
- Shortened the welcome copy so new users can understand it quickly.
- Onboarding now uses the dialog itself as the single scroll container to avoid Android WebView nested-scroll traps.
- Removed sticky setup buttons that could cover text on small screens.
- Dialogs remain vertically scrollable and use more of the available screen height.
- No finance storage keys, backup format, or security behavior changed.


## Income + onboarding improvements — 20.1.6

- App Lock setup is reduced to a short explanation: same phone PIN, same phone fingerprint, no separate My Bills PIN.
- Setup now has one completion action: `Finish & Add My First Bill`.
- Added Hourly / Salary choice to first-run setup.
- Salary users can enter annual salary plus an optional typical take-home check.
- If no take-home check is entered, weekly/biweekly salary checks are estimated from annual salary for projections.
- Salary is stored locally and shown in the income dashboard.
- The regular Add/Update Check Income dialog now supports Hourly and Salary income.
- Salary schedules ignore logged work hours so hourly tracking cannot overwrite salary checks.
- Existing hourly users and existing paycheck data remain compatible.


## PIN Lock behavior — 20.1.9

- PIN Lock is now OFF by default at the native Android layer as well as in setup.
- Quick app switches no longer force authentication again.
- PIN Lock only relocks after My Bills has been in the background for 5 minutes.
- Returning within 5 minutes keeps the current authenticated session open.
- Manual `Lock Now` behavior is unchanged and still locks immediately.


## Calculator fix — 20.3.0

- Replaced the old Function/eval-style calculator execution with a built-in arithmetic parser.
- The calculator now supports +, -, ×, ÷, decimals, unary negatives, and parentheses safely.
- Division by zero and malformed expressions still show Error.
- This keeps the stricter Content Security Policy intact instead of weakening security.


## Preservation baseline — 20.3.0

This build intentionally preserves the 20.1.11 visual design and main workflows.

Fixes:
- Page title/subtitle now follows the active bottom tab.
- Refund-like entries are normalized as Refund / Funds instead of Spending.
- Budget/work-hours controls are constrained to the phone width and wrap instead of overflowing.
- PIN Lock setup remains opt-in rather than preselected.
- Existing bills, payments, spending, work hours, budget, salary/hourly income, reminders, backup format, and navigation are retained.

This is the baseline for future cleanup: internal code can be simplified from here without redesigning the app.
