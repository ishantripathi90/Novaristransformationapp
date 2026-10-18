# Validation performed on the generated package

**Passed (local checks):**
- Account API PHP lint with PHP CLI.
- Account API health returns JSON; account registration (201), duplicate-registration rejection (409), wrong-password rejection (401), login (200), profile lookup (200), logout (200), revoked-token rejection (401).
- All eight original assessment entry forms load (HTTP 200).
- All eight forms accept their participant-registration step and redirect to the matching assessment PHP page (HTTP 200), including the anonymous Team Latitude route.
- Gradle wrapper bootstrap mechanism launched an isolated mock GradleMain with arguments, confirming its scripts / classpath handling without downloading a distribution.
- Android Kotlin source parsed without Kotlin syntax errors in a parser-only check; Compose/Android references could not be resolved because the Android SDK and Maven dependencies were not available in the authoring environment.
- The ZIP extracts without CRC errors and has standard 644 (regular file) / 755 (directory and executable script) permissions. The ZIP does not contain `.env` SMTP passwords.

**Not performed (needs your machine/hosting):**
- Actual Android Gradle sync, Kotlin/Compose compilation, APK build or emulator/physical-device tests. The authoring container has no Android SDK and no access to Google's Maven repositories.
- End-to-end original PDF generation and Titan SMTP mail delivery from an Android WebView on your deployed site.
- Live domain installation of the account API, because the hosting account has not been modified.

Do not treat these missing tests as passed. Use TEST-CHECKLIST.md before public distribution.
