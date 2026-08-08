# GlyphBoard Ω

**Every Unicode character, one keyboard.**

[![CI](https://github.com/Ranzlappen/phone-keyboard/actions/workflows/ci-android.yml/badge.svg)](https://github.com/Ranzlappen/phone-keyboard/actions/workflows/ci-android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

GlyphBoard is a full replacement Android keyboard (IME) with one twist: next to
the space bar sits an **Ω key** that opens a charmap-style browser of the
*entire* Unicode repertoire — every assigned character, in one continuous
scrollable grid, subdivided by Unicode block, exactly like the old Windows
`charmap` but built for a phone. Tap any character to type it into whatever app
you're in.

It installs alongside SwiftKey, Gboard, or any other keyboard. The 🌐 key swaps
back and forth instantly, so you can keep your daily driver and switch to
GlyphBoard whenever you need that one arrow, box-drawing character, rune,
or combining diacritic.

## Features

- **Full IME**: QWERTY letters, two symbol layers, shift with double-tap caps
  lock, long-press digits on the top row, key repeat on backspace, editor-aware
  enter key (search/send/go/done), automatic numeric layer for number fields.
- **The Ω browser**: every assigned Unicode character (~160k, grows with each
  Android release) in one scrollable grid with block headers — no bundled
  database, the catalog is built live from the device's ICU tables.
- **Jump index**: the ☰ button lists every block (Basic Latin → Supplementary
  Private Use) for instant navigation.
- **Search**: by official character name ("snowman", "combining acute") or by
  code point (`U+2603`, `0x1F600`, bare hex) — typed on an in-panel mini
  keyboard, since a keyboard can't summon a keyboard.
- **Recents**: your last 48 inserted characters at the top of the browser.
- **Detail card**: long-press any cell for the character's name, code point,
  and block, with copy-to-clipboard.
- **Sensible rendering**: combining marks show on a dotted circle ◌, invisible
  characters (ZWJ, no-break space…) show their hex code, and characters the
  device has no font glyph for are hidden by default (toggle in settings).
- **Custom layouts, unlimited**: swipe left/right on the space bar to cycle
  through any number of layouts — every one fully editable in the app,
  including the default QWERTY: per-key label, typed text, width, and
  SwiftKey-style hold popups (hold a key, slide across the variants, release
  to type — long lists wrap onto multiple popup rows). The editor shows a
  live tap-to-edit preview of the layout.
- **System function keys**: assign arrows, Home/End/PgUp/PgDn, Tab, Esc,
  F1–F12, copy/cut/paste/select-all, play/pause/next/prev, volume up/down/
  mute, Caps Lock, and one-shot **Ctrl/Alt modifiers** (Ctrl+C, Ctrl+Z…
  sent as real key events; shift+arrows selects text) to any key.
- **Similarity database**: an editable lookalike table covering a–z, A–Z,
  and 0–9 with hundreds of mappings — cross-script homoglyphs (Cyrillic,
  Greek, Cherokee, Lisu, small caps, IPA), all 13 mathematical styles
  (𝐛𝑜𝒍𝔡, 𝕕𝗈𝘂𝙗𝚕𝑒-𝖘𝓉𝔯𝗎𝚌𝑘…), fullwidth, circled, squared, super/subscripts.
  It feeds per-key hold popups (u → ʋ υ ᴜ 𝕦…) and…
- **Chaos mode** 🎲: a per-layout checkbox — type normally and every plain
  key press commits a *random* lookalike (ʜ𝚎ⅼˡ𝕠 ᴡ𝗈ʀӏď). Deliberate picks
  (hold popups, clipboard) stay exactly what you chose.
- **38+ preset layouts**: "Add layout" offers QWERTZ, AZERTY, Nordic and
  20+ language variants, Cyrillic (Russian/Ukrainian/Serbian…), Greek,
  Hebrew, Arabic, Persian, Georgian, Turkish F, plus Dvorak, Colemak(-DH),
  Workman, and Norman — each fully editable after adding.
- **Clipboard panel**: assign the 📋 key for an in-keyboard clipboard —
  current system clip plus locally pinned snippets, tap to insert. The
  clipboard is read only while the panel is open, never in the background.
- **Zalgo slider**: a per-key checkbox adds a vertical slider to the hold
  popup — drag up for live, increasingly cursed z̴̪̈a̶͖͂l̷̻̽g̸͚̈o̵̘̊ text. Or enable the
  per-layout **shift-key slider**: hold ⇧, set a level, and everything you
  type gets zalgo-fied until you set it back to zero.
- **Pinning**: pin single characters (long-press → Pin) into a section above
  Recents, or entire blocks (long-press a block name) right below it.
- **Quick switch button**: optional accessibility service that puts keyboard
  switching on the system accessibility button — one tap switches to
  GlyphBoard from anywhere (on Android 13+ it even *enables* GlyphBoard
  first if needed), tap again to hop straight back to the keyboard you came
  from. Declares zero data access: no events, no window content, nothing to
  read. (Android 13+ sideloads: allow restricted settings via App info → ⋮
  first — the app links you there.)
- **Private by design**: no INTERNET permission, no permissions at all, no
  autocorrect, no logging. What you type never leaves the input field.
- **Material 3** with dynamic color (Android 12+) and dark mode.

Deliberately **not** included (yet): autocorrect/suggestions, voice input,
translation, stickers, swipe typing.

## Install

1. Grab the latest APK from [Releases](https://github.com/Ranzlappen/phone-keyboard/releases)
   and install it (Android 8.0+ / API 26+).
2. Open GlyphBoard → **Open keyboard settings** → enable *GlyphBoard*.
3. **Choose keyboard** → pick GlyphBoard. Swap back to SwiftKey any time with
   the 🌐 key (long-press it for the full keyboard picker).

## Build from source

```bash
./gradlew assembleDebug          # Debug APK
./gradlew testDebugUnitTest      # JVM unit tests
./gradlew lintDebug              # Android Lint
./gradlew bundleRelease          # Release AAB (needs signing config)
```

Requires JDK 17+ and the Android SDK (platform 36). No NDK, no native code.

## Architecture

Single-module Kotlin app, Jetpack Compose everywhere — including inside the
IME service (`GlyphBoardService` implements the lifecycle/saved-state owners
Compose needs on a service window).

```
app/src/main/java/io/github/ranzlappen/glyphboard/
├── ime/            # InputMethodService + per-field UI state
├── ui/keyboard/    # Key model, layouts, key renderer, mode/shift state machine
├── ui/unicode/     # The Ω browser: grid, block index, search, detail card
├── ui/theme/       # Material 3 theme
├── data/unicode/   # Runtime Unicode catalog + name search (ICU-backed)
├── data/prefs/     # DataStore settings + recents
├── util/           # Pure-JVM code point helpers (unit-tested)
└── MainActivity.kt # Setup flow, test field, settings
```

The `util` and layout layers are pure Kotlin with no Android imports — the
seam a future Kotlin Multiplatform / iOS port would build on.

## Releases & CI

Every push to `main` runs lint + unit tests, builds debug APK and release
AAB/APK, then auto-tags the next patch version and publishes a GitHub Release
with the artifacts (`[skip release]` in the commit message skips it). Tag
`vX.Y.Z` manually for explicit versions. Release signing uses the
`KEYSTORE_BASE64` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`
secrets; without them, builds are signed by the **committed public fallback
keystore** (`signing/fallback.keystore`, password `glyphboard`) — it provides
no security, but keeps signatures stable so in-place updates work, and it
means every GitHub Release carries an installable release-variant APK.

Alongside the build workflow the repo runs:

- **Security scan** — CodeQL static analysis (Kotlin), gitleaks secret
  scanning, and an OpenSSF Scorecard, on every PR/push plus a weekly sweep.
- **Dependency review** — blocks any PR that pulls in a dependency with a
  known high/critical CVE.
- **Stale bot** — issue/PR housekeeping, shipped disabled (opt in with the
  `STALE_ENABLED=true` repository variable).
- **Dependabot** — weekly grouped updates for Gradle deps and GitHub Actions.

## License

[MIT](LICENSE)
