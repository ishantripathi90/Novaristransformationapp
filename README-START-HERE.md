# NOVARIS TRANSFORMATION LAB — Android Studio App Project

**A complete Android Studio source project plus the matching 8-assessment PHP website bundle.**

This is **not a prebuilt APK**. You build/install the APK in Android Studio. No payment system is included.

## What you get

- `NovarisAndroidStudio/` — native Android application written in Kotlin + Jetpack Compose, new mobile design, original Novaris triangle logo, approved Indian leadership photography, secure session storage, working login and signup when the server companion is installed, guest browsing, searchable dashboard with all eight assessments, recent-test shortcuts, profile/logout, WebView assessment runner and basic loading/error/retry UI.
- `SERVER_UPLOAD/` — the eight-assessment PHP website from your latest package, plus one new account API (`novaris-app-api.php`). The authored test pages and full PDF reporting logic are preserved. This folder contains **no `.env` credentials**.
- `ACCOUNT-API-ONLY/` — just the account PHP endpoint if you are already running the matching eight-assessment site.

## Fastest setup: two jobs

### 1. Put the matching assessment service on your website

Your live site must host the eight PHP assessments, or the Android app cannot run/submit them. Back up your existing hosting first. Upload **the contents** of `SERVER_UPLOAD/` directly to your site's `public_html/`, not the folder itself.

**IMPORTANT**: this ZIP deliberately DOES NOT include any private SMTP `.env` file. Keep your existing *working* `.env` securely on the host and don't overwrite it. If your hosting has no `.env` yet, set up SMTP following `SERVER_UPLOAD/.env.example` and `SERVER_UPLOAD/DEPLOYMENT.md`. In the existing code, complete PDF reports go to `lead@novaristransformationlab.com`; the participant only sees the executive summary.

Open these pages to check that the server is ready:

- `https://novaristransformationlab.com/assessments.php` — should list **eight** assessments.
- `https://novaristransformationlab.com/start-test.php?test=entronovaris` — registration page should load.
- `https://novaristransformationlab.com/novaris-app-api.php?action=health` — should show JSON with `"ok":true`.

If your old site is still live, do **not** skip this: the app can only open the assessments that actually exist on the server. Avoid leaving two versions of the site installed together. Extract the bundle into the correct document root. PHP 8.1+ and HTTPS are required for the account service.

### 2. Build the Android app

1. Install Android Studio (current stable) with **Android SDK API 35** and **JDK 17** (Android Studio's bundled JDK is fine).
2. Open Android Studio > **Open** > select the `NovarisAndroidStudio` folder.
3. Wait for Gradle Sync. An internet connection is needed to download Android build dependencies, and the wrapper downloads Gradle 8.10.2 on first use.
4. Plug in your Android phone with USB debugging, or create an emulator, and press the green **Run** button.
5. To create an installable debug APK: **Build > Build Bundle(s) / APK(s) > Build APK(s)**. The output is `NovarisAndroidStudio/app/build/outputs/apk/debug/app-debug.apk`.
6. For publishing, use **Build > Generate Signed App Bundle / APK** and create/keep your own signing keystore. The ZIP intentionally contains **no release signing key**.

You can also run `./gradlew assembleDebug` from the project root (`gradlew.bat assembleDebug` on Windows).

## Exactly how the assessment submission works

The home dashboard is a **native Android UI**. Tapping Start opens the matching original PHP assessment in an Android WebView at:

`https://novaristransformationlab.com/start-test.php?test=<assessment-key>`

This deliberately reuses the website's original questions, scoring, full-length original PDF generation, Titan email delivery and executive-summary receipt. The app DOES NOT build a second shortened PDF. Titan SMTP secrets and PDF attachments remain managed by the website, not included in the APK. When logged in, the participant's name/email/organisation are prefilled on eligible assessment registration forms; they can review/edit them. The **Team Latitude Map stays anonymous** and is **never prefilled with personal details**.

A test requires internet access and the PHP website to be working. The assessment session uses the site's PHP session cookie in the WebView; login is an independent app account, not a single-sign-on for the PHP website.

## Privacy, limitations and safety

- The account API hashes passwords with PHP `password_hash`; random bearer tokens are stored hashed server-side. Tokens on Android are encrypted using an Android Keystore AES-GCM key. The API requires HTTPS on non-local hosts and stores account files outside the public web root where possible.
- This version includes account signup/login/logout but **does not yet include email verification, password reset, account deletion or multi-device progress sync**. Add these and a privacy policy before any general-public Play Store release.
- `My journey` tracks **recently opened tests only**. It doesn't pretend a test was completed or store private scores.
- Team Latitude remains anonymous in the instrument and email by design, even if the app user is logged in.
- Full PDF creation and sending are **not confirmed on a live Android device or via your real Titan SMTP**. Test a complete trial submission to make sure WebView PDF generation, hosting PHP upload sizes and SMTP all work on the deployed server.
- The ZIP does not contain a precompiled Android APK because this build environment has no installed Android SDK or downloadable Android Gradle dependencies. Android Studio will build it on your own computer.
- The Gradle wrapper JAR is a small bundled bootstrapper that uses the official Gradle `distributionUrl` from wrapper properties (the official wrapper binary was unavailable in this offline environment). It only downloads the Gradle distribution; you can regenerate the official wrapper in a Gradle-equipped environment if desired.

## Customise your brand/app later

- App name / application ID: `NovarisAndroidStudio/app/build.gradle.kts` and `app/src/main/res/values/strings.xml`
- Website backend address: `BuildConfig.BASE_URL` in `NovarisAndroidStudio/app/build.gradle.kts`. The default is already `https://novaristransformationlab.com/`.
- Logo/images: `NovarisAndroidStudio/app/src/main/res/drawable-nodpi/`
- Test list: `NovarisAndroidStudio/app/src/main/java/com/novaris/transformationlab/Assessments.kt`
- Screens: `.../MainActivity.kt`
- Account API: `SERVER_UPLOAD/novaris-app-api.php`

For configuration or troubleshooting, read `TROUBLESHOOTING.md` and `TEST-CHECKLIST.md`.
