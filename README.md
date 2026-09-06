# My Bills 20.0.139

20.0.139 fixes the UI regression seen after the security update. The old legacy .app dashboard had become visible underneath the new tabbed interface. This build explicitly hides that legacy container after setupContent moves the useful live elements into #mb119Shell, and enforces exactly one visible tab at a time. The fixed navy headers, navy/cyan bottom navigation, spending editor, 20.0.137 finance logic, and native Android biometric/device-credential app lock from 20.0.138 are preserved.
