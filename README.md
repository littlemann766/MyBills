# My Bills 20.0.138

This build starts from 20.0.137 and adds native Android app security. My Bills opens to a locked screen and uses Android's biometric prompt on supported devices. Fingerprint/biometric authentication is preferred, with the phone's own PIN, pattern, or password as fallback. The app never receives or stores fingerprint data. It relocks after the app has been away for about 15 seconds. The Android bridge also exposes app-lock status, enable/disable, and Lock Now hooks for a future Settings control. All existing My Bills HTML/data behavior and the 20.0.137 theme remain intact.
