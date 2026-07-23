# GlyphBoard — Play Store launch plan

A phased checklist to take GlyphBoard from GitHub releases to Google Play.
Items marked **[manual]** need the maintainer (account/console actions);
items marked **[repo]** are code/CI work that can be done in this repo.

## Phase 0 — Prerequisites

- [ ] **[manual]** Google Play developer account ($25 one-time). New personal
      accounts need 12+ testers for 14 days in closed testing before
      production access — plan for that lead time or use an organization
      account.
- [ ] **[manual]** Decide the developer name shown on the listing.

## Phase 1 — Signing (do this first; it's permanent)

The committed `signing/fallback.keystore` is **public and must never sign a
Play build**. Play uploads need a private upload key:

- [ ] **[manual]** Generate a real keystore locally and keep it offline:
      `keytool -genkeypair -keystore release.keystore -alias glyphboard -keyalg RSA -keysize 4096 -validity 10000`
- [ ] **[manual]** Add the four repo secrets (`KEYSTORE_BASE64`,
      `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). CI already switches
      to real signing automatically when they exist.
- [ ] **[manual]** Enroll in **Play App Signing** at first upload (Google
      holds the app signing key; your keystore becomes the upload key —
      recoverable if lost).
- Note: Play installs will have a different signature than the fallback-signed
  GitHub builds — users migrating from GitHub releases must uninstall first.
  Mention it in the first Play release notes.

## Phase 2 — Policy compliance (the IME-specific part)

- [ ] **[manual]** **Privacy policy URL** — required for every app, and
      doubly scrutinized for keyboards. Host `PRIVACY.md` via GitHub Pages;
      content: no data collected, no network capability (no INTERNET
      permission), typing never leaves the device, similarity/layout data
      stays in local storage.
- [ ] **[manual]** **Data safety form**: "No data collected, no data shared."
      The reviewer can verify — the manifest requests zero permissions.
- [ ] **[repo]+[manual]** **AccessibilityService declaration**: the
      quick-switch service triggers Play's accessibility-API review. In the
      console's App content → Accessibility declaration, state: core purpose
      is a user-facing keyboard-switching shortcut; the service declares no
      event types, no capabilities, `canRetrieveWindowContent="false"`.
      The in-app card already gives the required prominent disclosure.
      **Fallback if rejected**: gate the service out of the Play build with a
      product flavor (`play` flavor without the service, `foss` flavor with
      it) — small build.gradle change, keep for plan B.
- [ ] **[manual]** Content rating questionnaire (IARC): utility, no
      user-generated content, no ads → Everyone.
- [ ] Target API: already `targetSdk 36` — comfortably above Play's minimum
      target-API requirement.

## Phase 3 — Store listing assets

- [ ] **[repo]** App icon 512×512 PNG (export the adaptive icon; script it
      with a small Gradle task or generate from `ic_launcher_foreground.xml`).
- [ ] **[manual]** Feature graphic 1024×500 (the Ω motif over a keyboard).
- [ ] **[manual]** 4–8 phone screenshots: QWERTY with hold popup open, the Ω
      browser with block index, search results, zalgo slider mid-drag, the
      layout editor, pinned sections. Dark + light.
- [ ] **[manual]** Short description (80 chars):
      "Every Unicode character on one keyboard — charmap browser, custom
      layouts." Long description: expand the README features; keywords:
      unicode keyboard, character map, special characters, IPA, zalgo.

## Phase 4 — Release engineering

- [ ] **[repo]** Bump `versionName` scheme if desired; Play consumes the AAB
      (`bundleRelease`) CI already builds and attaches to GitHub releases.
- [ ] **[manual]** Internal testing track first (up to 100 testers, instant),
      then closed testing (the 12-tester/14-day gate for new accounts), then
      production with staged rollout (10% → 50% → 100%).
- [ ] **[manual]** Watch the pre-launch report: Play's device farm will open
      the IME on ~10 devices — expect no crashes, but check the
      accessibility and input flags it raises.
- [ ] **[repo]** Optional later: a `publish-play.yml` workflow using
      `r0adkll/upload-google-play` with a service-account JSON secret to
      upload AABs to the internal track straight from CI.

## Phase 5 — Post-launch

- [ ] Crash/ANR monitoring via Play Console vitals (no SDK needed — keeps
      the no-network promise).
- [ ] Respond to the keyboard-specific review question if it comes: why no
      INTERNET permission is a *feature* (private by design).
- [ ] Keep GitHub releases as the F-Droid/power-user channel (fallback
      signing), Play as the mainstream channel (Play App Signing).

## Known review risks, ranked

1. **Accessibility service** — highest risk; mitigation above (declaration +
   flavor plan B).
2. **Keyboard category scrutiny** — IMEs get manual review; the zero-
   permission manifest is the strongest possible answer.
3. **New-account testing gate** — schedule the 14-day closed test before any
   launch date commitments.
