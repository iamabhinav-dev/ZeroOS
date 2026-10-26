# ZeroOS Launcher

<!-- in-app:start -->
ZeroOS is a minimalist Android home screen: plain text in black and white, with no icons, badges or feeds. It
shows the time, the date and the few apps you choose, and has optional screen-time limits, a Focus mode and a
short-video blocker. It is made by one person for personal use and is not on any app store.

## Before you install — read this
- Read this README, the [LICENSE](LICENSE) and the [third-party notices](THIRD_PARTY_NOTICES.md) first.
  **Installing ZeroOS means you have read and understood them and agree to them.** ZeroOS asks you to confirm
  this when it first starts, and again if a later version changes these documents.
- **Use at your own risk.** ZeroOS replaces your home screen and may cause problems (the phone hard to use,
  missed calls, notifications or alarms, lost settings, battery drain, conflicts with your phone's software),
  including problems not listed here or not known when it was released. You install it on your own judgement;
  the author and owner of this repository is not responsible for any damage or loss.
- **Personal use only.** Don't upload or share the APKs anywhere. To tell someone about ZeroOS, point them to this
  repository.

## Install
1. Use a phone with Android 8.0 or newer.
2. On the phone, download `zeroos-<version>-release.apk` from this repository's GitHub Releases page and open it.
   Allow your browser or file manager to install apps if Android asks. Play Protect may warn about an app from
   outside the store.
3. Press Home and choose ZeroOS, or set it in Settings → Apps → Default apps → Home app.

**Updating:** export your settings first (ZeroOS Settings → Backup → Export settings), then install the new APK
over the old one.

## Go back to your old launcher
Choose your old launcher in Settings → Apps → Default apps → Home app. Keep it installed: it's your fallback if
anything goes wrong. If ZeroOS keeps crashing, it starts in a simple safe mode with a button for this.

## What ZeroOS can access
- **No internet.** ZeroOS has no internet permission, no ads, no analytics and no accounts; everything stays on
  your phone.
- **Optional, off until you turn on the feature that needs it:** Usage access (screen time and limits),
  Accessibility (limits and breaks outside ZeroOS, the short-video blocker, double-tap to lock), and Do Not
  Disturb access (Focus). Each one is explained before Android asks.
- **Also used:** setting the wallpaper, opening alarms, asking to keep running in the background, and opening
  Android's uninstall dialog for an app you choose.
<!-- in-app:end -->

## For developers
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
| `python3 scripts/build-site.py` | Website into `build/site/` ([site/README.md](site/README.md)) |

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
scripts/          helper scripts (add-wallpapers.py, build-site.py)
site/             the GitHub Pages website (see site/README.md)
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
2. Copy `app/build/outputs/apk/release/app-release.apk` to the phone (USB or any file transfer).
3. Open it on the phone and install over the previous version.
   From v1.1 on (notification filter / Accessibility), Play Protect may block it: Play Store → profile → Play Protect → ⚙ →
   turn off "Scan apps with Play Protect", install, then **turn it back on** (D-018).
4. Run the manual check for the step, then follow the Install Checklist in IMPLEMENTATION_PLAN.md.

Rules: bump `versionCode` for every install; never install an APK built from older code than what's on the phone.

## If ZeroOS breaks (F18)
- **Never disable or uninstall the phone's stock launcher.** It's the fallback if ZeroOS can't start.
- After 3 crashes within 60 s of starting, ZeroOS opens in **safe mode**: a plain app list with
  "View crash report", "Open Home settings" and "Exit safe mode".
- To switch back to the stock launcher at any time: pull down the notification shade → ⚙ Settings →
  **Apps → Default apps → Home app** → the stock launcher (menu names differ between phones).
- Some phones' battery-saving modes switch the home app back to the stock launcher. Open ZeroOS from the app
  drawer and tap **Set as default** on the line at the top of home (F14.7).

## Licence
Personal use only. You may install the release APKs, or build ZeroOS yourself, for your own non-commercial use.
You may **not** upload or share the code or any APK anywhere: no app stores (Google Play, F-Droid, …), APK
sites, websites or file-sharing. See [LICENSE](LICENSE). Libraries and the Doto font keep their own licences:
see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and [LICENSES/](LICENSES/).
