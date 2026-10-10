# PINBOM · REBRAND NOTES

The app was built as **Bumo**; it is now **PinBom / پینبوم** — everywhere a user can
see it, and in the places they cannot, without losing their data.

---

## 1. What changed

| Area | Before | After |
|---|---|---|
| Package / namespace / applicationId | `ir.bumo.app` | `ir.pinbom.app` |
| Source roots | `app/src/{main,test}/java/ir/bumo/app` | `…/ir/pinbom/app` |
| Gradle project | `rootProject.name = "bumo-android"` | `"pinbom-android"` |
| Application class | `BumoApplication` | `PinBomApplication` |
| Design system | `BumoTheme`, `BumoColors`, `BumoTextStyles`, `BumoShape`, `BumoNavIcons`, `PinBomNavItem`, … | `PinBomTheme`, `PinBomColors`, `PinBomTextStyles`, `PinBomShape`, `PinBomNavIcons`, … |
| Theme resources | `Theme.Bumo`, `Theme.Bumo.Splash`, `bumo_window_background`, `bumo_splash_background` | `Theme.PinBom`, `Theme.PinBom.Splash`, `pinbom_*` |
| Brand drawables | `ic_bumo[_background\|_foreground\|_monochrome].xml` | `ic_pinbom[_…].xml` |
| Brand sources | `brand/bumo-{glyph,mark,background}.svg` | `brand/pinbom-{glyph,mark,background}.svg` |
| Strings (en) | app_name `Bumo`, “Search Bumo”, share links `https://bumo.app/…` | `PinBom`, “Search PinBom”, `https://pinbom.app/…` |
| Strings (fa) | `بومو` | `پینبوم` |
| Deep links | `bumo://pin/{id}`, `bumo://create`, `bumo://search` | `pinbom://…` **plus** the old scheme, kept as an alias |
| Docs | every `*.md`, the style guide, the Windows build script | renamed with the app |

Two passes, deliberately, not one blind search-and-replace: a case-sensitive
rewrite of `Bumo`/`BUMO_`/`bumo`/`بومو` across 120 files, then hand edits for the
four things a byte-level replace gets wrong (below).

---

## 2. What keeps the old name on purpose

1. **The `bumo://` scheme still resolves.** The launcher activity declares both
   `pinbom` and `bumo` intent filters, and `MainActivity` accepts either. A link
   already sitting in a chat, a note or a QR code opens the same screen it always
   did. The app's own shortcuts use `pinbom://`.
2. **The private files are migrated, not reset.** `StoreMigration` (run first in
   `PinBomApplication.onCreate`) renames `bumo_session` → `pinbom_session`,
   `bumo_preferences` → `pinbom_preferences` and `bumo-cache.db` →
   `pinbom-cache.db`, plus the SQLite `-wal`/`-shm` siblings. It never overwrites
   an existing file and never throws, so the worst case is the old behaviour:
   an empty store. An upgrade does **not** sign the user out and does **not**
   drop the offline cache.
3. **The notification channel id changed to `pinbom_uploads`**, and the old
   `bumo_uploads` channel is deleted once so Settings shows a single entry. The
   name users read comes from `R.string.upload_channel_name`.

---

## 3. Verification (this build)

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/opt/tc/sdk
./gradlew :app:assembleDebug        # BUILD SUCCESSFUL in 10m 21s
./gradlew :app:testDebugUnitTest    # BUILD SUCCESSFUL in 2m 32s — 14 tests, 0 failures
```

* `FirstRunSmokeTest`, `StartupSmokeTest` (5) — the real `MainActivity` starts and
  its first screen composes; `ResourcesSmokeTestApi24`/`Api35` (3 each) resolve
  every branded resource at both ends of the supported range; `ImageUrlTest` (2).
* APK inspection (`aapt2 dump badging`): `package: name='ir.pinbom.app'`,
  `application-label: 'PinBom'`, `application-label-fa: 'پینبوم'`,
  launchable activity `ir.pinbom.app.MainActivity`.
* Manifest inside the APK: schemes `pinbom` **and** `bumo`, hosts `pin`, `create`,
  `search`.
* `aapt2 dump resources … | grep -i bumo` → nothing: no user-visible occurrence of
  the old brand survives in the built resources.

## 4. Artifacts

| File | Bytes | MD5 |
|---|---|---|
| `/home/user/pinbom-1.0.0-debug.apk` | 23,966,099 | `e22116ccb01410bae689e3ce09bb25a6` |
| `/home/user/pinbom-android-1.0.0.zip` | source bundle — `pinbom-android/`, 204 entries | — |

The APK carries the debug certificate (`SHA-256 67a1e57a…`), so `adb install -r`
works alongside previous builds. The zip's own hash is reported at handover
rather than written here: a file cannot contain the hash of the archive that
contains it.
