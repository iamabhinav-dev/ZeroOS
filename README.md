# ZeroOS Launcher

A personal, minimalist, text-first Android launcher. Sideloaded on my own phone; no app store.

| Document | What it covers |
|---|---|
| [REQUIREMENTS.md](REQUIREMENTS.md) | What to build (feature and requirement IDs, defaults in Appendix A) |
| [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) | Build order, testing policy, Definition of Done |
| [AGENTS.md](AGENTS.md) | Rules for anyone (human or AI) changing this repo |
| [TRACEABILITY.md](TRACEABILITY.md) | Which tests cover each requirement |
| [docs/DECISIONS.md](docs/DECISIONS.md) | Decisions already made |
| [docs/wireframes.md](docs/wireframes.md) | Approved screen layouts |
| [CHANGELOG.md](CHANGELOG.md) | Changes per installed version |

## Setup (command line, no Android Studio)
- JDK 21 in `~/.local/jdk-21` (builds Java 17 bytecode; Java 21 is needed by Robolectric for API 37)
- Android SDK in `~/Android/Sdk` (cmdline-tools, platform-tools, `platforms;android-37.0`, `build-tools;37.0.0`)
- `~/.bashrc` sets `JAVA_HOME`, `ANDROID_HOME` and `PATH`

Gradle memory is capped in `gradle.properties` for an 8 GB machine.

## Everyday commands
| Command | What it does |
|---|---|
| `./gradlew check` | Everything that must pass before a commit: build, Android Lint, detekt (incl. ZeroOS rules), ktlint, all JVM tests, screenshot comparison, coverage ≥ 80% |
| `./gradlew ktlintFormat` | Auto-fix formatting |
| `./gradlew recordRoborazziDebug` | Re-record reference screenshots after an approved UI change |
| `./gradlew koverHtmlReportAggregate` | Coverage report in `build/reports/kover/htmlAggregate/` |
| `./gradlew assembleDebug` | Debug APK in `app/build/outputs/apk/debug/` |

## Project layout
```
app/              entry point, Hilt setup, diagnostics wiring
core/model        data models
core/ui           theme tokens and shared composables
core/resources    all user-facing text (strings.xml, plurals.xml)
core/logging      F17 diagnostics: Logger, log files, crash reports, exit reasons
core/data         repositories and storage
core/testing      test helpers (e.g. MutableClock)
feature/home      home screen
feature/applist   app list
build-logic/      Gradle convention plugins (shared module settings)
scripts/          helper scripts (add-wallpapers.py)
detekt-rules/     custom detekt rules (hardcoded text/values, direct logging)
config/           detekt config, permission allowlists, Robolectric settings
```

## Wallpapers (F6.8, D-038)
The built-in wallpaper gallery (Settings → Appearance → Phone wallpaper) always has **Pure black** and **Pure white**.
The other wallpapers are images that are **not in git**: anyone building ZeroOS makes them with an image-generator AI.

1. Open [docs/brand/wallpaper-prompts.md](docs/brand/wallpaper-prompts.md). Each design has a ready-made prompt for a
   dark and a light version.
2. Paste a prompt into an image generator (e.g. Gemini or ChatGPT) and save the result in `docs/brand/wallpapers/`
   with the file name given above the prompt: `wallpaper_<name>_dark.png` or `wallpaper_<name>_light.png`
   (portrait, ideally 1260×2800; PNG or JPG).
3. Run `python3 scripts/add-wallpapers.py` (needs `pip install pillow numpy scipy`). It finds every dot in each
   image and stores them as circles in `app/src/main/assets/wallpapers/`; the app draws them at the phone's exact
   resolution, so they stay sharp. It reports the size each adds to the APK, warns if the total gets too big, and
   lists any label still missing. (`--format webp` stores the images themselves instead: an exact copy, but
   enlarged on the phone and about three times bigger.)
4. For a new design, add its label to `core/resources/src/main/res/values/strings.xml` as
   `<string name="wallpaper_<name>" tools:ignore="UnusedResources">Name</string>` (the script prints the line).
5. Rebuild. No code changes are needed. To remove a wallpaper, delete its image and run the script again.

Dark wallpapers appear only in dark mode and light ones only in light mode. Without any images, the gallery simply
shows Pure black and Pure white.

## Signing (Step 0.7, D-010)
- Release key: `~/zeroos-keys/zeroos-release.jks` (alias `zeroos`), referenced by the git-ignored `keystore.properties`.
- Debug and release APKs are signed with this same key, so either installs over the other and keeps settings.
- Without `keystore.properties`, Gradle warns and signs with the default debug key; **never install those APKs on the phone**.
- Backups: the keystore and its password are kept off this computer (D-013). Never commit them.

## Installing on the phone
1. `./gradlew check assembleRelease`
2. Copy `app/build/outputs/apk/release/app-release.apk` to the phone (USB file transfer, Drive or Telegram).
3. Open it on the phone and install over the previous version.
   From v1.1 on (notification filter / Accessibility), Play Protect may block it: Play Store → profile → Play Protect → ⚙ →
   turn off "Scan apps with Play Protect", install, then **turn it back on** (D-018).
4. Run the manual check for the step, then follow the Install Checklist in IMPLEMENTATION_PLAN.md.

Rules: bump `versionCode` for every install; never install an APK built from older code than what's on the phone.

## If ZeroOS breaks (F18)
- **Never disable or uninstall the stock launcher.** It's the fallback if ZeroOS can't start.
- After 3 crashes within 60 s of starting, ZeroOS opens in **safe mode**: a plain app list with
  "View crash report", "Open Home settings" and "Exit safe mode".
- To switch back to the stock launcher at any time: pull down the notification shade → ⚙ Settings →
  **Apps → Default apps → Home app** → the stock launcher (menu names may differ slightly on the phone).

## Licence
Personal use only. You may install the release APKs, or build ZeroOS yourself, for your own non-commercial use.
You may **not** upload or share the code or any APK anywhere: no app stores (Google Play, F-Droid, …), APK
sites, websites or file-sharing. See [LICENSE](LICENSE). Libraries and the Doto font keep their own licences:
see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and [LICENSES/](LICENSES/).
