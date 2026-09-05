# My Bills v89

The Android app loads the exact same `index.html` that is included at the repository root.

## GitHub
Replace your repository's root `index.html` with this one.

## Android app
Replace your existing `app` folder and Gradle files with the files in this package, or at minimum copy the same `index.html` to:

`app/src/main/assets/index.html`

The WebView has JavaScript and DOM/local storage enabled so the app UI and calculations use the same HTML/JS code as the browser version.

## Build 116 hard reset
- Android versionCode 118 / versionName 19.1-v118
- Removed obsolete com.littlemann766.mybills MainActivity that loaded GitHub remotely
- Android WebView clears old cache and loads bundled assets only
- Service worker cache changed from legacy v61 cache-first to v116 network-first for HTML
- All three index.html copies synchronized

- Settings → App Version displays `117 / 19.1-v117` so installed/cached builds can be identified immediately.

- v118 COMPLETE: replaced all project/PWA/Android launcher icons with the no-tagline My Bills icon; authoritative navy/cyan theme; adaptive money tips; floating Settings removed; duplicate finance icons cleaned.
