# GlyphBoard (Android IME)

A Kotlin/Compose Android input method (system keyboard) whose signature
feature is a charmap-style browser of the entire Unicode repertoire, opened
with the Ω key. Repo name is `phone-keyboard`; product name is **GlyphBoard**;
package ID is `io.github.ranzlappen.glyphboard`.

## Architecture

Single-module Android app, no native code, no networking layer (by design —
see Key Conventions). Two entry points share one process and one DataStore:

* **`ime/GlyphBoardService`** — the `InputMethodService`. It implements
  `LifecycleOwner` + `ViewModelStoreOwner` + `SavedStateRegistryOwner` and
  installs itself as the view-tree owners on the IME window's decor view so a
  `ComposeView` can run inside the service. All text committing happens here
  (`performAction` / `insertCodePoint` on the current `InputConnection`);
  the Compose layer only reports `KeyAction`s.
* **`ui/keyboard/`** — pure-Kotlin key model + layouts (`KeyboardLayout.kt`),
  the key renderer (`KeyboardView.kt`, gesture handling: tap, long-press,
  backspace autorepeat), and the mode/shift state machine (`ImeRoot.kt`).
* **`ui/unicode/`** — the Ω browser (`UnicodeBrowser.kt`): one
  `LazyVerticalGrid` over the flat catalog item list with full-span block
  headers, a block jump-index overlay, name/code point search fed by an
  in-panel mini keyboard (an IME cannot summon an IME for its own text
  fields — never use a focusable `TextField` inside the keyboard window),
  and a long-press detail card. Overlays are plain in-panel `Box`es, never
  `Dialog`s (dialogs from IME windows need window-token workarounds).
* **`data/unicode/UnicodeCatalog.kt`** — builds the catalog at runtime from
  the platform ICU (`android.icu.lang.UCharacter` / `UnicodeSet`): every
  assigned code point except Cc/Cs/Cn/Co, grouped into blocks, optionally
  filtered by `Paint.hasGlyph`. Cached per filter flag; always built on
  `Dispatchers.Default`. No bundled Unicode data files.
* **`data/prefs/`** — DataStore-backed settings (haptics, hide-unsupported),
  the recents list, and pinned characters/blocks (pure, unit-tested codecs).
  All persistence shares the single DataStore in `GlyphDataStore.kt`.
* **`data/layouts/`** — the custom-layout model (`CustomLayout` etc.,
  kotlinx-serialization, pure Kotlin), the built-in QWERTY seed
  (`DefaultLayouts`), the JSON codec, `FnKey` (semantic system/function keys:
  arrows, F1–F12, clipboard, media/volume, Ctrl/Alt/CapsLock — persisted by
  enum name, so names are append-only), and `LayoutStore` (layout list = the
  space-swipe cycle order + active id, one JSON blob in DataStore).
  `ui/keyboard/LayoutConverter.kt` (also pure) turns a `CustomLayout` into
  renderable key rows, attaching shift/backspace and the bottom row. The IME
  service maps `FnKey` to key events (with Ctrl/Alt/Shift meta for combos and
  shift+arrow selection), `performContextMenuAction`, or `AudioManager`;
  Ctrl/Alt/CapsLock and the sticky shift-slider zalgo level are UI state in
  `ImeUiState`, applied in `ImeRoot.dispatch`.
* **`data/similarity/`** — the editable lookalike database behind the
  per-key "similar characters" popup checkbox; seeded from
  `SimilarityDefaults` (pure, tested).
* **`ui/keyboard/KeyPopup.kt`** — the hold-popup engine: `KeyPopupState`
  (candidate grid that wraps onto multiple rows, plus optional vertical zalgo
  slider, all geometry in root coordinates) and the overlay renderer. The
  pressed key keeps pointer capture and forwards drag positions; the overlay
  never handles input. **The overlay must be given `Modifier.matchParentSize()`
  — any size-dictating modifier (`fillMaxSize`) inflates the wrap-content IME
  window to full screen height** (this shipped as a bug once; don't repeat it).
* **`ime/KeyboardSwitchService`** — optional accessibility service bound to
  the system accessibility button for global keyboard switching. It must
  keep declaring **zero data access** (no event types, no window content).
* **`util/CodePoints.kt`, `util/Zalgo.kt`** — pure-JVM helpers.
* **`MainActivity.kt` + `ui/app/`** — setup flow (enable/select IME and the
  quick-switch service, with live status), test text field, settings, the
  layout editor, and the similarity-database editor. Dialog-based Compose is
  fine HERE (it's a normal activity) — never in the IME window.

## Build & Development

```
./gradlew assembleDebug              # Build debug APK
./gradlew assembleRelease            # Build release APK (needs signing config)
./gradlew bundleRelease              # Build release AAB
./gradlew installDebug               # Install on attached device
./gradlew testDebugUnitTest          # JVM unit tests
./gradlew lintDebug                  # Android Lint
```

There is no Android SDK in the usual Claude Code environments — CI is the
compile gate unless you install one.

## Key Conventions

* **No network, ever.** The manifest declares zero permissions. Adding
  INTERNET (or any permission) breaks the product's core privacy promise and
  needs an explicit maintainer decision first.
* **The IME never spies**: no logging of committed text, no analytics.
* **Unicode data comes from the platform ICU at runtime** — do not bundle
  UnicodeData.txt or similar. `Character.getName` / `android.icu` provide
  names and blocks; the catalog grows automatically with OS updates.
* **Keep `util/` and `ui/keyboard/KeyboardLayout.kt` free of Android imports.**
  They are the unit-tested, KMP-portable core for a future iOS port.
* **Compose-in-IME rules**: view-tree owners are set on the decor view in
  `onCreateInputView`; lifecycle is driven from `onWindowShown`/`onWindowHidden`;
  `onEvaluateFullscreenMode` returns false. Don't introduce `Dialog`-based
  Compose components (`ModalBottomSheet`, `AlertDialog`, `DropdownMenu`) into
  the IME window.
* **Debug builds install alongside release** (`applicationIdSuffix .debug`),
  so both keyboards can be enabled at once while testing.
* **`signing/fallback.keystore` is intentionally public** (password
  `glyphboard`): it signs debug builds and secretless release builds so every
  build shares a stable signature and CI releases always ship an installable
  release APK. It provides zero security — never treat it as a secret, and
  never use it once real `KEYSTORE_*` secrets exist.
* **Keep `util/`, `KeyboardLayout.kt`, `LayoutConverter.kt`, and the
  `data/layouts` model files free of Android imports** (kotlinx.serialization
  is fine — it's KMP).
* **Package ID is permanent**: `io.github.ranzlappen.glyphboard`.
* **Score one flat grid**: the browser is intentionally a single scrollable
  list subdivided by block headers (charmap model), not per-block pages.

## Deployment & CI/CD

| Workflow | Trigger | Scope | Deploys |
| --- | --- | --- | --- |
| `ci-android.yml` | push to `main`, pull_request to `main`, tag `v*`, workflow_dispatch | Source paths (markdown, `LICENSE`, `.gitignore` excluded via `paths-ignore`) | Artifacts on every run; auto-tags + publishes a GitHub Release on every push to `main` (patch bump from latest tag; first release is v1.0.0) and on explicit `v*` tags. Every release carries debug APK, release APK (real-signed with secrets, fallback-signed without), and AAB. `[skip release]` in the commit message skips the auto-release. |
| `security-scan.yml` | PR + push to `main`, weekly cron, `branch_protection_rule`, dispatch | Whole repo | CodeQL (`java-kotlin`, `build-mode: manual` — compiles the app with `assembleDebug` so the extractor sees real bytecode), gitleaks secret scan, and OpenSSF Scorecard (Scorecard runs on push/schedule/dispatch only). Results land in the Security tab. |
| `dependency-review.yml` | pull_request to `main` | Dependency manifest changes | Per-PR gate; flags a PR that introduces a high/critical CVE and comments the diff. Requires the repo's **Dependency graph** to be enabled — until then the step is `continue-on-error` (non-blocking); remove that line to make it a hard gate. |
| `stale.yml` | daily cron, dispatch | Issues + PRs | **Disabled by default** — only runs when repo variable `STALE_ENABLED=true`. Marks/closes stale issues (60/7 days) and PRs (90/14 days); dependency PRs exempt. |

**Concurrency**: `ci-${{ github.ref }}`, `cancel-in-progress: true`.

**Runtime versions**: JDK 17 (Temurin), Android SDK 36, Gradle 9.5.1
(wrapper), AGP 9.2 (built-in Kotlin — no standalone kotlin-android plugin),
Kotlin 2.3.

**Required secrets** (signed release APK/AAB; optional — CI passes without
them and ships debug-signed APK + unsigned AAB):

| Secret | Purpose |
| --- | --- |
| `KEYSTORE_BASE64` | Base64 of `release.keystore` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Signing key alias |
| `KEY_PASSWORD` | Signing key password |

## Tech Stack

| Layer | Technology | Why |
| --- | --- | --- |
| Language | Kotlin 2.3 | Android primary; KMP path for iOS later |
| UI | Jetpack Compose + Material 3 | Single UI toolkit for app *and* IME |
| IME | `InputMethodService` + Compose view-tree owners | Standard system keyboard API |
| Unicode data | Platform ICU (`android.icu`) | Zero bundled data, auto-updates with OS |
| Persistence | `androidx.datastore.preferences` | Settings + recents |
| Build | Gradle 9.5.1, AGP 9.2 | Matches sibling repos |
| CI | GitHub Actions | Matches Ranzlappen/repo-standards |

## Project Structure

```
phone-keyboard/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/io/github/ranzlappen/glyphboard/
│       │   └── res/                 # strings, themes, method.xml, adaptive icon
│       └── test/                    # JVM unit tests (pure-Kotlin layers)
├── gradle/ (libs.versions.toml, wrapper)
├── .github/ (workflows/ci-android.yml, dependabot.yml, templates)
├── build.gradle.kts / settings.gradle.kts / gradle.properties
├── CLAUDE.md
└── README.md
```

## Post-task self-check

After every turn that produces code or workflow changes, scan for:

- New Gradle dependencies → add to `gradle/libs.versions.toml`, not inline.
- New permissions → almost certainly wrong for this app; stop and reconsider.
- Android imports leaking into `util/` or `KeyboardLayout.kt` → move them out.
- Dialog-based Compose components in the IME window → replace with overlays.
- New CI secrets → document in this file and README.
- README "Features" / this file kept in sync with behavior changes.

If nothing applies, say "no doc/workflow updates needed."
