# My Bills v61 — Full Test Package

## Web/PWA files
- `index.html` — current My Bills app
- `manifest.webmanifest` — installable-app metadata
- `sw.js` — offline cache/service worker
- `icons/` — app icons

For browser/PWA testing, host this folder over HTTPS or localhost.
Opening `index.html` directly from Downloads (`content://` / `file://`) can test most app features,
but Android will not reliably deliver background scheduled notifications after the page is closed.

## Android bridge reference
`android_bridge_reference/` contains a starter native Android WebView bridge showing how the existing
`window.AndroidBridge` hooks can connect to Android notification permission and test notifications.

The native scheduler method is intentionally left as a reference hook because exact background scheduling
should be implemented with AlarmManager/WorkManager in the Android project before release.

## Good things to test
1. Month swipe on dashboard, calendar, and bill menus.
2. Bills, recurring occurrences, paid/partial statuses, payment history.
3. Hours on calendar, paychecks, YTD totals.
4. Budget, spending, savings goals, monthly snapshot.
5. Save to App Memory / Restore Last Save / rolling backups.
6. Category dropdown, calculator, filters, recurring manager.
7. Notification sound preview while the app is open.
8. Offline behavior after first successful load from a hosted origin.

## Important notification limitation
Browser/download mode cannot guarantee exact alarms or custom notification sounds when closed.
Those capabilities require the Android wrapper/native notification bridge.
