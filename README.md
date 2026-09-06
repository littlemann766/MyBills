# My Bills 20.0.140

20.0.140 fixes the Java compilation failure from the first biometric implementation. The app now uses Android FingerprintManager for fingerprint authentication and falls back to the phone's native PIN, pattern, or password screen when fingerprint is unavailable. The fingerprint itself is still handled by Android and is never stored by My Bills. The 20.0.139 UI shell regression fix and the existing finance/theme behavior are retained.
