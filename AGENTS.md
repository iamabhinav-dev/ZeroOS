# Agent Guidelines — ZeroOS Launcher

Rules for any AI agent (or human) working in this repository. **Read this file before making changes.**

**Project context:** a personal launcher for one user. It's tested on one primary phone, but code must work on any Android phone. The app is installed by sideloading a signed APK. **No app store, no monetization, no ADB, no Developer options.** Don't propose or build anything that needs these.

## 1. Sources of Truth
| File | Purpose |
|---|---|
| [REQUIREMENTS.md](REQUIREMENTS.md) | *What* to build. Feature IDs (F1–F16) and requirement IDs (e.g., F8.3, NFR-P1). |
| [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) | *In what order* to build it. Phases and steps (e.g., Step 2.4). |
| [TRACEABILITY.md](TRACEABILITY.md) | Which tests cover each requirement ID. |
| [docs/DECISIONS.md](docs/DECISIONS.md) | Decisions already made. Don't reopen them; ask if one seems wrong. |
| docs/wireframes.md | Approved screen layouts, added phase by phase. **Never build a screen without an approved wireframe.** |
| AGENTS.md (this file) | *How* to work. |

If these files conflict, **stop and ask the user**. Do not resolve it yourself.

## 2. Scope Rules (most important)
1. **Work on one plan step at a time.** Identify the current step in IMPLEMENTATION_PLAN.md before writing code. Do not start the next step until the current step's "Done when" checklist is met.
2. **Build only what is in REQUIREMENTS.md.** No extra features, screens, settings, animations, or "nice to haves" unless the user asks for them.
3. **Do not build ahead.** Don't add code, modules, dependencies, or permissions for future phases "to save time later".
4. **Do not edit REQUIREMENTS.md or IMPLEMENTATION_PLAN.md** unless the user asks you to. If you find a gap or a problem, report it and propose a change instead.
5. **Ask when it's unclear.** If a requirement is ambiguous or seems impossible on Android, ask before guessing. Note any assumption you make in the commit message.
6. **No unrelated refactors.** Only touch code needed for the current step. Report other problems you notice; don't fix them silently.
7. **Every change must map to an ID.** If you can't name the requirement (e.g., F2.4) or plan step (e.g., Step 2.4) a change serves, don't make it.
8. **Use the defaults in REQUIREMENTS.md Appendix A.** Don't invent default values, thresholds, or limits.
9. **Removed or deferred items stay out:** F15 (premium), F12.1 (weather), F16.13 (remote rules), ADB-based features, store/release work, translations/RTL, and device/emulator tests, unless I ask for them.
10. **Only public Android APIs.** No OEM-private APIs, reflection into hidden APIs, root, or device-specific code paths.
11. **Keep safe mode (F18) minimal and independent.** Never make it depend on DataStore, Room, services, or feature modules.

## 3. Commits — Small and Incremental
- **Commit after each logical unit of work**, not at the end of a phase. One step usually means several commits.
  - Good: "add AppsRepository with LauncherApps callback", then "add app list screen", then "add search matching".
  - Bad: one commit containing a whole phase.
- **Before each commit:** new code has tests and `./gradlew check` passes (build, lint, detekt/ktlint, all JVM tests, coverage), once the Android project exists.
- **Message format** ([Conventional Commits](https://www.conventionalcommits.org/)) with the requirement/step ID:
  ```
  feat(applist): add initials-based search matching

  Implements F2.4 (Step 2.4).
  ```
  Types: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`, `build`, `ci`, `perf`.
- **Branches:** one branch per plan step, e.g. `step-2.4-app-search`; merge into `main` when the step is done.
- **Never** commit secrets, keystores (`*.jks`), `keystore.properties`, `local.properties`, APKs, or build output.
- **Keep the repo public-ready** (it's private, but may go public; D-046):
  - Commit only with the GitHub noreply address (`git config user.email` in this repo); never a personal email.
  - No personal data in tracked files or commit messages: no emails, phone numbers, real names of other people,
    screenshots with personal content, app-usage logs, the user's phone model or brand, measurements from it,
    or the apps and preferences they use. Write "the primary phone" / "the test phone" instead.
  - Personal facts go **only** in the git-ignored `PERSONAL.md` (D-048); read it for context, never copy from it.
  - Screen dumps and test fixtures keep structure only, never on-screen text.
  - Keep `LICENSE` (personal use only, no uploading anywhere), `THIRD_PARTY_NOTICES.md` and `LICENSES/` up to
    date: a new library in the APK gets a row (and its licence text if it isn't there yet).
  - Every GitHub Release starts with the "Personal use only" notice linking `LICENSE`.
- **Pushing:** after a step is merged into `main`, ask me before pushing to the private GitHub repo. **Never** force-push, rewrite history, or open PRs without my approval.
- A step is finished only when the **Definition of Done** in IMPLEMENTATION_PLAN.md is met. Then mark it ✅ in IMPLEMENTATION_PLAN.md in its own `docs:` commit.

## 4. Technical Conventions
- **Stack:** Kotlin, Jetpack Compose (Material 3, restyled to minimal), MVVM + unidirectional data flow, single activity, Hilt, DataStore (from Phase 2), Room and WorkManager (from Phase 4), Coroutines/Flow.
- **Module placement** follows IMPLEMENTATION_PLAN.md (`core/*`, `feature/*`). Feature modules must not depend on each other; shared code goes in `core/`.
- **Dependencies:** add a library only if the current step needs it; use the version catalog (`gradle/libs.versions.toml`); prefer AndroidX and Jetpack. Mention every new dependency in the commit message.
- **UI:** no app icons, badges, or colorful elements, which goes against the product's purpose. Use theme tokens from `core/ui` (no hardcoded colors, sizes, spacing, or durations).
- **Text (NFR-L3–L7):** every user-facing text goes in `core/resources` (`strings.xml` / `plurals.xml`); never hardcode text in Kotlin or Compose. **Search for an existing string before adding one** and reuse it (`common_*` for shared text, `<screen>_*` for screen-specific text). Use placeholders and plurals, never concatenation. The build fails on hardcoded text.
- **Accessibility:** content descriptions, 48dp touch targets, WCAG AA contrast (NFR-A1–A4).
- **Landscape:** every screen must work in portrait **and** landscape: nothing cut off, buttons reachable, no
  oversized art. Screens that don't scroll use a two-pane layout when wider than tall. Every new or changed
  screen gets a landscape screenshot test (`@Config(qualifiers = "w872dp-h392dp-land-xxhdpi")`, named
  `landscape_*`) that is looked at before committing.
- **Performance:** no polling loops, no work on the main thread, and use system callbacks (NFR-P1–P6).
- **Time:** never call `System.currentTimeMillis()` or `LocalDate.now()` directly in logic; inject a `Clock`/`TimeSource` so tests can control time.
- **State:** survive process death and config changes (`SavedStateHandle`, persisted settings) (NFR-R7).
- **Logging (F17):** use only the `Logger` from `core/logging`; never `android.util.Log` or `println`. Every new feature must log its key events (F17.7) and every caught exception at WARN/ERROR. **Never log** the private content in F17.8 (notification text, intention text, search queries, to-do text, custom app names, custom block message, other apps' screen content); free-text setting values are logged as `<redacted, N chars>`. Logs stay on the device (NFR-S5).

## 5. Privacy & Permissions (non-negotiable)
- No analytics, ads, crash-reporting services, or trackers. **No network access at all**: never add the `INTERNET` permission (NFR-S3).
- **Do not add a permission to the manifest** unless the current step's requirement needs it. Update `config/permissions-allowlist.txt` in the same commit with the requirement ID; the allowlist test enforces this.
- Sensitive permissions (Usage Access, Notification Access, Accessibility, DND) must be opt-in, have an explanation screen (including the Android 13+ "Allow restricted settings" instructions where relevant), be requested only when needed, and have a working fallback if denied.
- All user data stays on the device.

## 6. Testing (mandatory)
- **No step is done without tests.** Follow the *Testing Policy* in IMPLEMENTATION_PLAN.md and the step's **Tests** list.
- **JVM only for now:** unit tests, Compose UI tests on Robolectric, Roborazzi screenshot tests, and Robolectric persistence/WorkManager tests. Don't add instrumented, emulator, or benchmark tests unless I ask.
- Write tests **alongside the code**, ideally in the same commit as the logic they cover.
- Bug fixes start with a failing regression test.
- Every schema change (DataStore proto, Room) gets a migration test.
- Time-dependent logic is tested at midnight, time-zone change, DST change, and manual clock change.
- Name tests after the requirement ID (e.g., `fun f2_4_matchesInitials()`) and update TRACEABILITY.md.
- Keep logic coverage ≥ 80% (Kover).
- Don't delete, skip, or weaken existing tests to make a build pass. Fix the cause or ask.
- If a step needs a manual phone check, say so in the report. Don't claim it passed: I do it on the phone.

## 7. When You Finish a Task
Report briefly:
1. Which step and requirement IDs were done, and which tests cover them.
2. What was committed (list of commits).
3. Anything skipped, assumed, or blocked, and open questions.
4. What I need to check manually on the phone, if anything.
5. The next step in the plan, without starting it unless asked.
