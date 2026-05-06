# Eduksha — Play Store release checklist

End-to-end runbook for shipping the Android app to Google Play. Read top to
bottom the first time. After the first release the day-to-day flow is just
the **Per-release** section near the bottom.

---

## Phase 1 — One-time setup (do this once, ever)

### 1. Generate the upload keystore

This is the key Google Play uses to verify uploads from your machine. **You
will be locked out of updating the app forever if you lose this file**, so
back it up to two places.

```bash
cd /path/to/repo
keytool -genkey -v -keystore eduksha-upload.jks \
  -alias eduksha -keyalg RSA -keysize 2048 -validity 10000
```

Use a strong password. Write it down somewhere safe (1Password, encrypted
text file outside the repo, etc.).

### 2. Wire it into the build

Copy the template and fill in the password you just chose:

```bash
cp keystore.properties.template keystore.properties
# edit keystore.properties — replace the two CHANGE_ME values
```

Both `eduksha-upload.jks` and `keystore.properties` are in `.gitignore`.
**Never commit them.**

Verify it works:

```bash
./gradlew :app:bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab` — this is what you
upload to Play.

### 3. Register SHA-1 in Firebase

Get your upload keystore's SHA-1:

```bash
keytool -list -v -keystore eduksha-upload.jks -alias eduksha
```

Copy the `SHA1` line. Open the Firebase console:

https://console.firebase.google.com/project/dobi-ac085/settings/general/

→ Your apps → Android app `com.dobi.app` → **Add fingerprint** → paste SHA-1.

Phone-OTP login uses this fingerprint to sign request tokens. Without it,
release-build OTP silently fails.

### 4. Enable Play App Signing & enroll

In Play Console → **Setup → App integrity → App signing** → opt in. Google
generates an **app signing key** and stores it for you; your keystore
becomes just the **upload key**. After enrollment:

1. Play console gives you a second SHA-1 (the *App signing certificate*).
2. Add **that** SHA-1 to Firebase the same way as step 3.
3. From now on, OTP login works for both side-loaded AABs and Play-store
   installs.

### 5. Lock down App Check on production

Right now your debug device uses `DebugAppCheckProviderFactory`. The release
classpath uses `PlayIntegrityAppCheckProviderFactory` (already wired in
`DobiApplication.kt`). To enforce it server-side:

1. Firebase console → **Build → App Check → Apps**.
2. For each protected service (Firestore, Cloud Functions, Cloud Storage)
   click **Enforce**. Once enforced, requests without a valid Play
   Integrity attestation are rejected — debug builds keep working because
   you've registered the debug provider key.
3. Verify by uninstalling + reinstalling the app from Play and confirming
   functions still work.

### 6. Lock down Cloud Functions security

Both `getDailyNews` and `deleteAccount` are currently set to "Allow public
access" at Cloud Run, which is fine — the in-function `request.auth` check
still gates real users. Once App Check enforcement (step 5) is on, you can
optionally flip them to **Require authentication**:

https://console.cloud.google.com/run?project=dobi-ac085 → click the service
→ **Security** tab → "Require authentication" → Save.

### 7. Tighten Firestore rules

The repo's `firestore.rules` is now production-ready (uploads, homeworks,
subjective_results, quiz_attempts, dobi_meta, invites all locked down).
Deploy:

```bash
firebase deploy --only firestore:rules
```

After deployment, smoke-test by signing in as a parent and confirming you
can still: take a quiz, upload homework, view past topics. Each of those
exercises a different rule.

---

## Phase 2 — Play Console listing

### 8. Create the app on Play Console

https://play.google.com/console → **Create app**.

- **App name:** Eduksha
- **Default language:** English (India) — `en-IN`
- **App / Game:** App
- **Free / Paid:** Free (you can introduce paid features later via IAP)
- **Declarations:** check the developer program policies, US export laws.

### 9. Fill the store listing

| Field | What to use |
|---|---|
| Short description (≤80 chars) | Daily learning for Indian families. Homework, AI quizzes, kid-safe news. |
| Full description (≤4000 chars) | Crib from the landing page copy on the website. |
| App icon (512×512 PNG) | Use the same launcher icon at 512×512 — export from Android Studio's Image Asset Studio or upscale `mipmap-xxxhdpi/ic_launcher.png` losslessly. |
| Feature graphic (1024×500) | Hero card — gradient background, "EDUKSHA" wordmark, tagline. |
| Phone screenshots (≥2, ≤8) | Capture: ChildHome, My Learnings (with feed), Eduksha Times newspaper, Profile, a Quiz screen, Streak dashboard. Use a real device or Pixel emulator at 1080×2400. |
| App category | Education |
| Tags | Education, Family, Learning |
| Contact details | `hello@eduksha.in`, your phone, postal address (must be real — Play shows it publicly). |
| Privacy policy URL | `https://dobi-ac085.web.app/privacy` (swap to `https://eduksha.in/privacy` once the GoDaddy hold lifts). |

### 10. Account deletion URL

Play Console → **App content → Account deletion** → set:
- In-app deletion: **Yes** (Profile tab → Delete account).
- Web URL for deletion request: `https://dobi-ac085.web.app/delete-account`.

### 11. Data Safety form

Play Console → **App content → Data safety**. Be honest; mismatches with
your privacy policy get apps removed. Quick reference for Eduksha:

- **Personal info collected:** Name (child's first name), Phone (parent
  OTP).
- **Photos:** Yes (homework / self-learning uploads).
- **Files / docs:** No.
- **Health & fitness:** No.
- **App activity:** App interactions (for streaks / progress).
- **Device IDs:** Yes (Play Integrity App Check).
- **All collected data is encrypted in transit:** Yes.
- **Users can request data deletion:** Yes (in-app + web).
- **Data shared with third parties:** Google (Firebase, Gemini API). No
  advertising / analytics SDKs.

### 12. Content rating

Play Console → **App content → Content rating** → fill the IARC
questionnaire. For Eduksha you should land at **PEGI 3 / ESRB Everyone /
IARC 3+**. Critical: answer "no" to every "user-generated content shared
publicly" question — friend connections in Eduksha are 1-to-1 only and
don't expose any public profile.

### 13. Designed for Families

Play Console → **App content → Designed for families**. Since Eduksha is
directed at children under 13:

- Opt **in** to the Designed for Families program.
- Target age: 6–8, 9–12 (cover both).
- Confirm: no third-party advertising, no in-app purchases targeted at
  children, no cross-promo to non-Family-certified apps.
- Reviewers actually open the app — they need a working test account. See
  step 14.

### 14. Reviewer test credentials

Play Console → **Setup → App content → Sign-in instructions or test
credentials**.

Indian phone-OTP login won't work for foreign reviewers. Pre-create one of:

- A test parent account using a real number you control + provide the OTP
  through a hard-coded debug bypass code in `DEBUG_BUILDS_ONLY` (don't
  ship in release).
- Or a "demo mode" toggle that lets reviewers explore the app without
  signing in.

Document whichever path you choose in the test-credentials field. Reviewers
read these instructions verbatim.

### 15. App permissions justification

Play Console asks why the app needs each permission:

- **Camera / READ_MEDIA_IMAGES** — to let the child take homework photos.
- **POST_NOTIFICATIONS** — to notify parent of pending reviews and child of
  new tasks (FCM push).
- **RECEIVE_BOOT_COMPLETED** — required by FCM for redelivery after reboot.
- **INTERNET** — core service.

---

## Phase 3 — Test track rollout

### 16. Internal testing

Play Console → **Testing → Internal testing → Create new release**.

Upload `app/build/outputs/bundle/release/app-release.aab`. Add yourself as
a tester (and any family members willing to smoke-test). Internal track
goes live in ~10 minutes.

Smoke-test on a real device:

- [ ] Sign-in OTP works.
- [ ] Add child + child PIN.
- [ ] Upload homework photo (reads Firestore + Storage, writes uploads).
- [ ] Take a curated MCQ quiz; score saves and shows on Today's Learning.
- [ ] Open the Newspaper overlay; pages flip.
- [ ] Profile shows streak / topics / quizzes.
- [ ] Delete account → confirm everything wipes; can re-onboard cleanly.
- [ ] Crash logs show up in Crashlytics if you've enabled it.

### 17. Closed testing

Once internal smoke passes, move to **Testing → Closed testing**. Add 5–20
real parents/kids (allowlist by email). Run for at least 3–7 days; collect
their feedback before promoting to production.

### 18. Production release

Play Console → **Production → Create new release** → reuse the same AAB or
upload a new one. Roll out to **20% staged**, watch crash rate for 24h,
then ramp to 100%.

First-time review usually takes **3–7 days**. Expect at least one
back-and-forth from Google on the Data Safety form or Designed-for-Families
questionnaire — fix promptly and re-submit.

---

## Per-release flow (for every update after launch)

1. Bump in `app/build.gradle.kts`:
   ```kotlin
   versionCode = N + 1
   versionName = "1.x.y"
   ```
2. `./gradlew :app:bundleRelease`
3. Upload AAB to whichever track (Internal / Closed / Production).
4. Most updates pass review in 2–24 hours. Sensitive permission changes
   re-trigger the full review.

---

## Already done in code

- Cloud Functions: `getDailyNews`, `deleteAccount`, `createInvite`,
  `claimInvite`, `onParentTaskWrite`, `pendingReviewReminder`.
- App-side delete account flow (Profile tab → confirmation dialog → wipe).
- Release signing config wired through `keystore.properties`.
- R8 / minify enabled with `proguard-rules.pro` keep rules covering
  Firestore data classes, kotlinx-serialization nav routes, and enums.
- Hardened `firestore.rules` matching every collection the app actually
  writes.
- Marketing site at `https://dobi-ac085.web.app` with privacy, terms,
  delete-account, support pages.

## Still to do (manual)

- [ ] Generate keystore (Phase 1, step 1).
- [ ] Add SHA-1 to Firebase (Phase 1, step 3).
- [ ] Enroll Play App Signing + add second SHA-1 (Phase 1, step 4).
- [ ] Enforce App Check on Firestore + Functions + Storage (Phase 1, step 5).
- [ ] `firebase deploy --only firestore:rules` (Phase 1, step 7).
- [ ] Create Play Console listing + screenshots + feature graphic.
- [ ] Data Safety form, Content rating, Designed for Families.
- [ ] Provide reviewer test credentials.
- [ ] Submit Internal → Closed → Production.
