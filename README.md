# My Bills v89

The Android app loads the exact same `index.html` that is included at the repository root.

## GitHub
Replace your repository's root `index.html` with this one.

## Android app
Replace your existing `app` folder and Gradle files with the files in this package, or at minimum copy the same `index.html` to:

`app/src/main/assets/index.html`

The WebView has JavaScript and DOM/local storage enabled so the app UI and calculations use the same HTML/JS code as the browser version.
