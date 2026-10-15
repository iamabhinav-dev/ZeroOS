# Agent Guidelines — ZeroOS Launcher

Rules for any AI agent (or human) working in this repository. **Read this file before making changes.**

## 1. Sources of Truth
| File | Purpose |
|---|---|
| [REQUIREMENTS.md](REQUIREMENTS.md) | *What* to build. Feature IDs (F1–F16) and requirement IDs (e.g., F8.3, NFR-P1). |
| [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) | *In what order* to build it. Phases and steps (e.g., Step 2.4). |
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

## 3. Commits — Small and Incremental
- **Commit after each logical unit of work**, not at the end of a phase. One step usually means several commits.
  - Good: "add AppsRepository with LauncherApps callback", then "add app list screen", then "add search matching".
  - Bad: one commit containing a whole phase.
- **Before each commit:** the project builds, lint passes, new code has tests, and all tests pass (`./gradlew assembleDebug lint test` once the Android project exists).
- **Message format** ([Conventional Commits](https://www.conventionalcommits.org/)) with the requirement/step ID:
  ```
  feat(applist): add initials-based search matching

  Implements F2.4 (Step 2.4).
  ```
  Types: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`, `build`, `ci`, `perf`.
- **Branches:** one branch per plan step, e.g. `step-2.4-app-search`; merge into `main` when the step is done.
- **Never** commit secrets, keystores, `local.properties`, or build output.
- **Never** push, force-push, rewrite history, or open PRs without the user's approval.
- After finishing a step, mark it done in IMPLEMENTATION_PLAN.md by adding ✅ to the step heading, in its own `docs:` commit.

## 4. Technical Conventions
- **Stack:** Kotlin, Jetpack Compose (Material 3, restyled to minimal), MVVM + unidirectional data flow, single activity, Hilt, DataStore, Room (from Phase 4), Coroutines/Flow, WorkManager.
- **Module placement** follows IMPLEMENTATION_PLAN.md (`core/*`, `feature/*`). Feature modules must not depend on each other; shared code goes in `core/`.
- **Dependencies:** add a library only if the current step needs it; use the version catalog (`gradle/libs.versions.toml`); prefer AndroidX and Jetpack. Mention every new dependency in the commit message.
- **UI:** no app icons, badges, or colorful elements, which goes against the product's purpose. Use theme tokens from `core/ui` (no hardcoded colors or sizes) and keep all user-facing text in `strings.xml`.
- **Accessibility:** content descriptions, 48dp touch targets, WCAG AA contrast (NFR-A1–A4).
- **Performance:** no polling loops, no work on the main thread, and use system callbacks (NFR-P1–P6).

## 5. Privacy & Permissions (non-negotiable)
- No analytics, ads, or trackers. No network calls except those REQUIREMENTS.md allows (weather, billing, signed F16 rules).
- **Do not add a permission to the manifest** unless the current step's requirement needs it.
- Sensitive permissions (Usage Access, Notification Access, Accessibility, Device Admin, DND) must be opt-in, have an explanation screen, be requested only when needed, and have a working fallback if denied.
- All user data stays on the device.

## 6. Testing (mandatory)
- **No step is done without tests.** Follow the *Testing Policy* in IMPLEMENTATION_PLAN.md and the step's **Tests** list.
- Write tests **in the same branch and alongside the code**, ideally in the same commit as the logic they cover. Tests are not a later phase.
- Unit tests for all logic; Compose UI tests for every new/changed user flow; instrumented tests for services, Room/DataStore, and permission fallbacks.
- Bug fixes start with a failing regression test.
- Every requirement ID you implement must be covered by at least one test; name or annotate tests with the ID (e.g., `fun f2_4_matchesInitials()`).
- Keep logic coverage ≥ 80% (Kover/JaCoCo).
- Don't delete, skip, or weaken existing tests to make a build pass. Fix the cause or ask.

## 7. When You Finish a Task
Report briefly:
1. Which step and requirement IDs were done, and which tests cover them.
2. What was committed (list of commits).
3. Anything skipped, assumed, or blocked, and open questions.
4. The next step in the plan, without starting it unless asked.
