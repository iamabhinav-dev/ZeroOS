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
detekt-rules/     custom detekt rules (hardcoded text/values, direct logging)
config/           detekt config, permission allowlists, Robolectric settings
```

## Installing on the phone
Signing and the install workflow are set up in Step 0.7 and documented here then.
