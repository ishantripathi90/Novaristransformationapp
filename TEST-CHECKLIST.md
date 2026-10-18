# Before release: practical manual test checklist

## Android UI
- [ ] App launches cleanly on Android 8 (API 26) or later.
- [ ] Sign up with real name and email -> opens the dashboard.
- [ ] Sign out -> returns to auth screen; old token no longer works.
- [ ] Sign in with same account -> opens dashboard.
- [ ] Sign in with wrong password -> shows clear error.
- [ ] Explore as guest -> assessment list usable, without creating an account.
- [ ] Search and category filters show the correct set of eight assessments.
- [ ] Profile shows account email or guest state.
- [ ] Recent-tests page lists opened assessments, not fabricated results.
- [ ] Rotate device and try narrow-screen Android phone.

## Tests and reports (requires deployed server and working Titan SMTP)
- [ ] Entronovaris opens, completes and sends complete original PDF to Novaris.
- [ ] AuthorityFlow opens, completes and sends complete original PDF to Novaris.
- [ ] Growth-Readiness Matrix opens, completes and sends complete original PDF to Novaris.
- [ ] RELATIQ opens, completes and sends complete original PDF to Novaris.
- [ ] Team Dynamics Map opens, completes and sends complete original PDF to Novaris.
- [ ] Team Latitude Map remains anonymous, including its mailed report.
- [ ] MEridian sends **two** full-length originals (report + development plan).
- [ ] EQuilibrium sends **two** full-length originals (report + development plan).
- [ ] Participant sees only the short executive summary.
- [ ] Slow internet + retry does not falsely mark submission successful.
- [ ] Participant details are only prefilled when logged in, except for anonymous Team Latitude.

## Backend security
- [ ] Account API accessed over HTTPS.
- [ ] Private account store cannot be read at any web URL.
- [ ] Original `.env` still private and SMTP credentials not present in Android APK/project.
- [ ] Rate limits reject repeated login guessing.
- [ ] Release APK/App Bundle is signed with your private signing key.
- [ ] Privacy policy, account deletion, email verification and password reset are planned before a public Play Store release.
