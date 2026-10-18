# Troubleshooting

### App signup says "Account service isn't ready"
- Confirm `https://novaristransformationlab.com/novaris-app-api.php?action=health` returns JSON with `"ok":true` (not HTML, login redirect or 404).
- Copy the **contents** of SERVER_UPLOAD directly into the document root used by your domain.
- Verify HTTPS and PHP 8.1+ enabled, and site/parent folder writable for creation of the private accounts storage.
- Check hosting PHP error logs if HTTP 500 is returned.

### App shows homepage but a test won't start
- The hosted version must have all 8 PHP assessment endpoints, not the older Novaris PACES site.
- Confirm `start-test.php?test=authorityflow` and other test keys load in your normal browser.
- Use the deployed `SERVER_UPLOAD` PHP files (not the Android Studio folder) on your hosting.

### WebView stuck on assessment loading
- Ensure Android System WebView/Chrome is updated.
- Test the **same assessment registration and submission** in mobile Chrome.
- Verify your hosting allows POST multipart uploads and enough PHP memory/upload size for the original report PDFs.
- Consult `SERVER_UPLOAD/ORIGINAL-REPORT-DELIVERY.md` and `SERVER_UPLOAD/PDF-RENDER-REPAIR.md`.
- A lost connection, PDF rendering error, or Titan SMTP failure can keep a submission pending. There is no reason to claim it succeeded without an actual mail-delivery response.

### No report email
- Reports use the existing PHP website's Titan SMTP configuration, not the app API.
- Keep the valid `.env` on your web host; the distribution ZIP excludes secrets.
- See server PHP error logs and the original delivery documentation.

### Gradle sync error
- Install Android API 35 and SDK Build Tools in SDK Manager.
- Use Android Studio's bundled JDK 17 or 21.
- Internet is necessary to download the Gradle distribution and AndroidX dependencies.
- If your network blocks Gradle distribution downloads, install Gradle 8.10.2 manually and use Android Studio's local Gradle distribution option.
- Clean/re-sync project after dependencies download.

### Account data folder
- First choice: a private `novaris-app-private/` folder **one directory above your public_html**.
- Only if the host prohibits this does the PHP API create a fallback folder inside public_html with filesystem `0700` and `.htaccess` deny rules. Do not remove those protections; on non-Apache hosts you should configure an explicit deny rule or relocate private storage above the web root.
- Account email addresses and password hashes are private user data. Keep backups secure and avoid exposing PHP error logs.
