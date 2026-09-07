# My Bills 21.0.0

This is the clean-foundation rewrite of My Bills.

## Architecture

- `index.html` — semantic app shell only.
- `app.css` — one stylesheet.
- `app.js` — one authoritative application controller/data layer.
- Android continues to load the bundled local app from `app/src/main/assets/`.
- Root web files and Android asset files are identical.

## Compatibility

The rewrite intentionally continues reading and writing the established My Bills localStorage keys for bills, payments, paychecks, work hours, spending/refunds, budgets, reminders, income type, hourly wage, and salary. Existing installed-app data can therefore carry into 21.0.0.

## Foundation rules

- No mutation-observer patch stack.
- No eval / Function calculator execution.
- No duplicate navigation controllers.
- No account or Internet permission.
- PIN Lock is optional and handled by Android.
- Android Back closes the currently open dialog first.
- New-user setup is resumable.
- Core features are separated by page and rendered from one state model.

This release is a foundation reset. Add future features to the clean state/render/event structure instead of appending version patches.
