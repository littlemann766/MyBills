# My Bills — Public Release Checklist

## Build and Android
- [x] Application ID remains `com.mybills.app`.
- [x] Permanent release signing key retained.
- [x] Target Android 16 / API 36.
- [x] Compile with API 36.
- [x] GitHub Actions creates a signed APK.
- [x] GitHub Actions creates a signed Android App Bundle (`.aab`).
- [ ] Confirm the current release installs over the previous release without data loss.
- [ ] Confirm fresh install works with no existing data.
- [ ] Test Android 16 behavior.
- [ ] Test Android 13–15 compatibility.

## Functional regression
- [ ] Home dashboard totals are correct.
- [ ] Calendar navigation and bill editing work.
- [ ] Recurring bills work.
- [ ] Paying/editing a bill updates totals correctly.
- [ ] Spending includes paid bills.
- [ ] Refund / Funds reduces net spending.
- [ ] Budget calculations remain correct.
- [ ] Backup export succeeds.
- [ ] Backup restore succeeds on a clean install.
- [ ] Notifications/reminders work.
- [ ] App lock works.
- [ ] Android Back behavior remains correct.
- [ ] App works offline.

## Security/privacy
- [x] No Android INTERNET permission.
- [x] Cleartext traffic disabled.
- [x] Automatic Android backup disabled.
- [x] WebView debugging disabled.
- [x] External web pages cannot run inside the bridged WebView.
- [x] Mixed content and universal file access disabled.
- [ ] Dedicated user-input/XSS review.
- [ ] Decide whether app-level encryption is needed before making any encryption claim.
- [ ] Publish final privacy policy at a stable public URL.
- [ ] Put privacy policy link/text inside the released app.

## Google Play
- [ ] Create/verify Play Developer account.
- [ ] Enroll in Play App Signing.
- [ ] Upload `MyBills.aab` to internal testing first.
- [ ] Complete Data safety accurately.
- [ ] Complete Financial features declaration.
- [ ] Complete content rating.
- [ ] Add support contact and privacy-policy URL.
- [ ] Create store icon, feature graphic, screenshots, short/full description.
- [ ] Run closed testing if required for the developer account.
- [ ] Resolve pre-launch report issues before production.
