# My Bills 20.0.148

20.0.148 is the cleanup and security-foundation release. It intentionally keeps the existing My Bills features and local data format while reducing repository baggage and hardening the Android wrapper.

## Android identity and updates

- Application ID: `com.mybills.app`
- Permanent release signing alias: `mybills`
- Current version: `20.0.148` (`versionCode 148`)
- Existing My Bills app data remains in the same Android application sandbox and the web data/storage keys are unchanged.

## Required GitHub Actions secrets

- `MYBILLS_KEYSTORE_HEX`
- `MYBILLS_KEYSTORE_PASSWORD`

Do not commit the keystore, its HEX representation, or its password to this repository.

## Security foundation in 20.0.148

- Android cleartext network traffic is disabled.
- The Android app no longer requests the INTERNET permission because the production wrapper uses bundled local assets.
- Android automatic app-data backup is disabled; My Bills uses its explicit user-controlled backup/restore feature instead.
- WebView debugging is explicitly disabled.
- File-URL cross-origin access and universal file access are disabled.
- Mixed HTTP/HTTPS content is blocked.
- Third-party cookies are disabled.
- External web pages cannot load inside the WebView that owns the Android JavaScript bridge; web links are handed to the device browser.
- A restrictive Content Security Policy blocks external scripts, frames, objects, and unexpected network connections while allowing the app's existing inline code.

## Cleanup in 20.0.148

- The active `MainActivity.java` now lives in the conventional `com/mybills/app` source path.
- Old placeholder Java source files and the unused `app/src/index.html` stub were removed.
- Obsolete v121-v125 PWA icon generations were removed from Android assets.
- Root and Android `index.html` are synchronized.
- The 45 accumulated inline style blocks were consolidated into one ordered `mybills.css` file without changing the visual rules.
- PWA icon references now consistently use the current v126 artwork.
- Service-worker, README, upload notes, and build metadata are aligned to 20.0.148.

## Next production step

After this cleanup release is installed and tested, move the Android build to API 36+, add an Android App Bundle (`.aab`) release artifact, and begin store-readiness testing. The large legacy single-file UI should be refactored feature-by-feature only after a verified backup and regression baseline, rather than by deleting patch layers in one risky pass.
