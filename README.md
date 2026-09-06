# My Bills 20.0.143

20.0.143 introduces permanent Android APK signing.

The Android application ID remains `com.mybills.app`. GitHub Actions now reconstructs the same private release keystore from repository Actions secrets and builds `assembleRelease`, producing a consistently signed `MyBills.apk`.

Once 20.0.143 is installed, future releases signed with this same key can be installed directly over the existing app, preserving Android app storage.

Important: an APK already installed from the older changing/debug signing key cannot be updated by the new permanent key. Back up My Bills data first, uninstall the old build one final time, install 20.0.143, restore the backup, and then keep updating in place from 20.0.144 onward.

Required GitHub Actions repository secrets:
- `MYBILLS_KEYSTORE_BASE64`
- `MYBILLS_KEYSTORE_PASSWORD`

The signing key alias is fixed as `mybills`.
