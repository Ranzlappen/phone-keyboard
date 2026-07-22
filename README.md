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
- **Private by design**: no INTERNET permission, no permissions at all, no
  autocorrect, no logging. What you type never leaves the input field.
- **Material 3** with dynamic color (Android 12+) and dark mode.

Deliberately **not** included (yet): autocorrect/suggestions, voice input,
translation, stickers, swipe typing, additional language layouts.

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
secrets; without them CI still passes and ships a debug-signed APK plus an
unsigned AAB.

## License

[MIT](LICENSE)
