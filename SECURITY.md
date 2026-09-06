# My Bills Security Notes

## Current model

My Bills is local-first. Bills, income, spending, settings, and related finance data are stored in the app's WebView storage on the device. The app does not require the Android INTERNET permission in 20.1.5. User backups are created only through the explicit backup/export flow.

## Android protections

- App sandbox remains the primary at-rest isolation boundary.
- Automatic Android backup is disabled.
- Optional app lock uses the device fingerprint/device credential mechanisms already present in the app.
- The JavaScript bridge is reachable only from the bundled Android asset page; external web navigation is blocked from the in-app WebView.
- WebView debugging, universal file-URL access, mixed content, and third-party cookies are disabled.
- Cleartext Android network traffic is disabled.

## Important limitation

The finance database is not separately encrypted with an app-specific encryption key. It is protected by the Android application sandbox and device security. Before a broad public release, decide whether app-level encryption is required for the intended threat model and store claims.

## Release-key handling

The permanent signing key is part of the app's long-term identity. Keep `MYBILLS_KEYSTORE_HEX` and `MYBILLS_KEYSTORE_PASSWORD` only in protected GitHub Actions secrets and a secure offline recovery location. Never commit either secret into the repository or distribute them with the APK/project ZIP.

## Before public release

1. Target the Play-required Android API level and test behavior changes.
2. Build and verify a signed Android App Bundle as well as the APK.
3. Run fresh-install, update-in-place, backup/restore, notification, app-lock, and offline tests.
4. Publish a privacy policy that accurately describes local storage, backups, notifications, and any future network features.
5. Complete Google Play Data safety declarations from the actual shipped behavior.
6. Perform a dedicated input/XSS review before adding any network sync, accounts, or remote content.


## 20.1.5 production note
The Android build now targets API 36 and produces an Android App Bundle for Play testing. The app remains intentionally offline/local-first. Adding Internet permission, remote content, analytics, advertising, cloud sync, or accounts should trigger a new privacy/security review.


## 20.1.5 authentication wording
The onboarding and Settings UI explicitly state that Android device authentication is used. My Bills does not create, receive, or store a separate user PIN, device passcode, or fingerprint template.
