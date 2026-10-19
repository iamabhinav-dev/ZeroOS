# ZeroOS Launcher — Implementation Plan

Step-by-step build plan for the features in [REQUIREMENTS.md](REQUIREMENTS.md).
Each step lists **what to build**, **key technical notes**, **tests**, and a **done when** checklist. Steps are ordered so the app is installable and usable after every phase.

**Context:** personal app, tested on one primary phone, installed by sideloading a signed APK. No ADB, no Developer options, no app store.

Time estimates are **full-time-equivalent for one developer**; scale to your own pace.

| Phase | Scope | Est. time | Outcome |
|---|---|---|---|
| 0 | Setup, wireframes, feasibility spikes | 1–1.5 weeks | Project builds; risky APIs proven on the phone |
| 1 | Prototype (M0) | 1 week | Usable as default launcher |
| 2 | v1.0 features | 3–4 weeks | F1–F6, F13, F14 complete |
| 3 | Daily-driver hardening | 1 week | v1.0 installed as my default launcher |
| 4 | v1.1 Wellbeing & insights | 5–6 weeks | F8, F9, F10 (F7 deferred, D-039) |
| 5 | v1.2 Extras | 1 week | F11, F12 |
| 6 | v1.3 Short-form blocker | 2–3 weeks | F16 |
| **Total** | | **~4 months** | |

---

## Testing Policy (applies to every step)
Tests are **required**, not a separate phase. For now, **all automated tests run on the JVM only** (no phone or emulator needed). A step is **not done** until:

1. **Unit tests** cover all new logic: repositories, ViewModels, matching/filtering, calculations, parsers, and state reducers.
2. **Compose UI tests** (run on Robolectric) cover every new or changed user flow.
3. **Screenshot tests** (Roborazzi) cover every new or changed screen in dark + light theme and at default + largest font scale.
4. **Accessibility checks** are enabled in Compose UI tests where supported (content descriptions, touch target size).
5. **Persistence tests** (Robolectric): DataStore/Room read/write, and a **migration test** for every schema change.
6. **Time-dependent logic** uses an injected `Clock`/`TimeSource` and is tested for: local midnight, time-zone change, DST change, and manual clock change.
7. **Regression test first** for every bug fix: write a failing test, then fix.
8. **Permission allowlist test**: a unit test compares the merged manifest's permissions to `config/permissions-allowlist.txt` (all builds) plus `config/permissions-allowlist-debug.txt` (debug-only tools, e.g. LeakCanary). Adding a permission means updating the allowlist in the same commit, with the requirement ID.
9. `./gradlew check` (build, lint, detekt/ktlint, all JVM tests, coverage verification) passes locally before every commit.
10. Coverage of `core/` and `feature/*` logic (excluding UI-only composables) stays **≥ 80%** (Kover) from Phase 1 onward.
11. [TRACEABILITY.md](TRACEABILITY.md) maps each implemented requirement ID to its tests.

**Deferred (not required for now):** instrumented tests, emulator tests, Macrobenchmark/Baseline Profiles. These are listed in *Deferred Work* at the end and can be re-enabled later.

### Manual phone check (for steps that change UI or Android services)
1. Build and install the APK on the primary phone (see *Install workflow*, Step 0.7).
2. Check the step's "Done when" items by hand.
3. Note the result in the step's final commit message (`Manual check: passed on the phone`).

---

## Definition of Done (every step)
- [ ] All requirement IDs for the step are implemented, and nothing else.
- [ ] Tests from the Testing Policy are written and pass (`./gradlew check`).
- [ ] TRACEABILITY.md is updated.
- [ ] Screenshots are updated, and wireframe differences are approved by me.
- [ ] Manual phone check is done (if applicable).
- [ ] CHANGELOG.md has an entry under "Unreleased".
- [ ] New decisions are recorded in docs/DECISIONS.md.
- [ ] Step heading marked ✅ in this file.

---

## Phase 0 — Setup, Wireframes & Feasibility

### Step 0.1 — Tooling ✅
- **No Android Studio** (needs too much RAM for this 8 GB machine; D-017). Command-line setup instead:
  - JDK 21 (Temurin 21.0.12.1) in `~/.local/jdk-21`. Originally JDK 17; switched in Step 0.6 because Robolectric needs Java 21 for API 37. Bytecode still targets Java 17.
  - Android SDK in `~/Android/Sdk`: cmdline-tools (latest), platform-tools, `platforms;android-37.0`, `build-tools;37.0.0`
  - `JAVA_HOME`, `ANDROID_HOME`, and `PATH` set in `~/.bashrc`
- Editor: VS Code (Kotlin extension optional). Screens are reviewed via Roborazzi screenshot PNGs instead of Studio previews.
- Gradle memory capped in `gradle.properties` (Step 0.2): Gradle ≤ 2 GB, Kotlin daemon ≤ 1 GB, limited workers, Robolectric tests in one fork with capped heap.
- compileSdk/targetSdk 37, min SDK 26.
- On the phone: nothing yet. Later, allow "Install unknown apps" for the app used to open the APK (Files/Drive). No Developer options.

### Step 0.2 — Create the project ✅
- New project → *Empty Activity (Compose)*, package `com.zeroos.launcher`.
- Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`).
- Add only what Phase 0–1 needs: Compose BOM, Material 3, Lifecycle, Coroutines, Hilt. Add DataStore in Phase 2, Room/WorkManager in Phase 4.
- Enable R8/minify for release builds.

### Step 0.3 — Module structure ✅
Start with modules needed now; add feature modules when their phase begins.
```
app/
core/model/
core/ui/
core/resources/   # all user-facing text (NFR-L3)
core/logging/     # F17
core/data/
core/testing/
feature/home/
feature/applist/
```

### Step 0.4 — Wireframes ✅
- Create `docs/wireframes.md` with ASCII/low-fi wireframes. **Wireframes are approved phase by phase:** each phase starts with a *Wireframes* step for its new screens, and I approve them before any screen in that phase is built.
- In this step: wireframes for the Phase 1 screens (app list, diagnostics, safe mode) and the **final home screen** (below).
- The **home wireframe shows its final layout**: clock, date, battery, screen-time stats (each F10.5 style), focus button / focus timer, calendar/to-do lines, and favorites. It also shows which areas collapse when a feature is off, and how it fits at the largest font size.
- **I review and approve them before Phase 1.** Agents build screens to match approved wireframes; differences need my approval.

### Step 0.5 — Feasibility spikes (throwaway, on a `spike/` branch, not merged)
A small debug APK installed on the phone to check the risky parts **before** building on them:
1. **App list**: `LauncherApps` returns all apps, including App Clone apps if any.
2. **Usage stats**: `UsageStatsManager` totals for 2–3 apps vs. the phone's own screen-time screen over 1 day.
3. **Notification listener**: survives overnight and after a reboot with the phone's default battery settings, and again with background activity allowed.
4. **Accessibility service**: can be enabled after "Allow restricted settings"; survives overnight; a debug **node-tree dumper** saves YouTube Shorts and Instagram Reels screen structures to a file (these become F16 test fixtures, with no ADB needed).
5. **Overlay**: `TYPE_ACCESSIBILITY_OVERLAY` shows a black screen over YouTube.
6. **Play Protect install block**: install the spike APK (which declares a notification listener and an Accessibility service) from a file manager or other app with Play Protect **on**. Record whether it is blocked, then confirm the workaround (pause Play Protect scanning → install → turn it back on) works (D-018).

Record findings in `docs/DECISIONS.md`. If any spike fails, **stop and discuss** before continuing the plan.

### Step 0.6 — Quality gates & test setup ✅
- ktlint + detekt; Android Lint with warnings as errors.
- **Text and token checks (NFR-L3–L8):** Lint `HardcodedText` as error; a custom detekt rule that fails on string literals passed to `Text()`, `contentDescription`, `label`, `placeholder`, and similar UI parameters, and on hardcoded `Color(…)`/`.dp` values outside `core/ui`. `core/resources` holds `strings.xml`/`plurals.xml` with the `common_*` / `<screen>_*` naming convention.
- Test stack: JUnit, MockK or hand-written fakes (`core/testing`), Turbine, Robolectric, Compose UI test, Roborazzi; Kover with an 80% verification rule.
- Permission allowlist test (Testing Policy #8), starting with an empty allowlist.
- Debug builds only: LeakCanary and StrictMode (`detectAll().penaltyLog()`).
- **Diagnostics core (F17.1–F17.8, F17.12)** in `core/logging`, built now so every later step can use it:
  - `Logger` API with levels; file writer to `filesDir/logs/` on a background thread with buffering; rotation (3 × 1 MB) and 7-day cleanup.
  - Uncaught-exception handler writing `crash-<timestamp>.txt` with the last 50 entries, then calling the default handler.
  - Previous exit reasons read on start (Android 11+).
  - Debug builds also mirror logs to Logcat.
  - A detekt/lint rule that forbids `android.util.Log` and `println` outside `core/logging`.
- One sample unit test, one Robolectric UI test, one screenshot test to prove the setup.
- `.gitignore` (incl. `keystore.properties`, `*.jks`), `README.md`, `CHANGELOG.md`.
- Optional: GitHub Actions running `./gradlew check` if the repo is pushed to GitHub later.

### Step 0.7 — Signing & install workflow ✅
- Generate a release keystore **outside the repo**; reference it from a git-ignored `keystore.properties`. Back it up (NFR-S6).
- **One key for debug and release:** both build types use the same `signingConfig` (the release keystore) and the same application ID, so either APK installs over the other and keeps settings. If `keystore.properties` is missing (e.g., on CI), fall back to the default debug key and print a Gradle warning; such APKs must not be installed on the phone.
- Debug-only tools (LeakCanary, StrictMode) run on the phone whenever a debug APK is installed; that's expected.
- Database/DataStore schemas only migrate forward. Never install an APK built from **older code** than what's on the phone: it can fail to start or need a reset. Debug and release share one `versionCode` sequence.
- Versioning: `versionCode` +1 per install; `versionName` = milestone (e.g., `1.0.3`). Android refuses to install a lower `versionCode` over a higher one, so a rollback = rebuild old code with a **higher** `versionCode`.
- Document the install workflow in `README.md`:
  1. `./gradlew check assembleRelease`
  2. Copy `app/build/outputs/apk/release/*.apk` to the phone (USB or any file transfer)
  3. Open it on the phone and install over the previous version. From Phase 4 on (the app declares notification-listener/Accessibility services), Play Protect may block the install: pause it first (Play Store → profile → Play Protect → ⚙ → "Scan apps with Play Protect" off), install, then **turn it back on** (D-018)
  4. Run the manual check

### Step 0.8 — Off-machine backups
- **Code:** create a **private** GitHub repository and push `main` (I approve the first push). After that, push `main` whenever a step is merged.
- **Keystore:** store the release keystore and its passwords **outside the repo and off this computer** (e.g., password manager or encrypted cloud drive). Never in the GitHub repo, not even private.
- Note both locations (not the secrets) in README.

**Done when:** `./gradlew check` passes with sample tests; wireframes are approved; spike results are recorded with no blockers; a signed release APK installs on the phone; code is on the private GitHub repo and the keystore backup is verified (restore it to a temp folder and build with it).

---

## Phase 1 — Prototype (M0): A Working Launcher

> **Exception (D-024):** Phase 1 may start before Step 0.5 phone results arrive; Step 0.5 must be finished before Phase 2 starts.

### Step 1.1 — Register as a home app ✅
In `AndroidManifest.xml`, declare `MainActivity` with:
- intent filter: `MAIN` + `HOME` + `DEFAULT` categories
- `launchMode="singleTask"`, `excludeFromRecents="true"`, `stateNotNeeded="true"`, `clearTaskOnLaunch="true"`
- `<queries>` block with a `MAIN`/`LAUNCHER` intent (package visibility on Android 11+).

Behaviour:
- Pressing Home while already on the launcher → `onNewIntent` closes the drawer and returns to home.
- Back on home → does nothing.

### Step 1.2 — Load installed apps ✅
- `core/model`: `AppEntry(packageName, activityName, user: UserHandle, label, customLabel?, isHidden)`; unique key = component + user serial.
- `core/data/AppsRepository`:
  - `LauncherApps.getActivityList(null, user)` for every profile in `UserManager.userProfiles`.
  - `LauncherApps.Callback` → `Flow<List<AppEntry>>` updating on install/uninstall/update.
  - Sort with a locale-aware `Collator`.
- Launch via `LauncherApps.startMainActivity(component, user, ...)`.

### Step 1.3 — Bare-bones UI ✅
- Home: big clock + date via a `Flow` that ticks each minute, aligned to the minute boundary (injected `Clock`).
- Swipe up → scrollable list of app labels; tap launches.

### Step 1.4 — Diagnostics screen (F17.9–F17.11) ✅
- Minimal screen: recent logs (filter by level), crash reports, Export/Share (zip via share sheet or save-to-file), Clear.
- Until Settings exists (Phase 2), open it by long-pressing empty space on home; in Step 2.11 it moves to Settings → Diagnostics and long-press opens Settings (F1.9).
- One-time "crashed last time" message on the next start after a crash.
- Log the Phase 1 events: app start, home/list shown, app launched, package changes.

### Step 1.5 — Safe mode (F18) ✅
- Start/crash counter in a small synchronous file; crash handler (F17.5) increments it.
- Trigger at 3 crashes within 60 s of start; minimal safe-mode screen in `app/` with plain app list and the F18.3 buttons.
- README: "Never disable or uninstall the stock launcher" and how to switch back (Settings → Apps → Default apps → Home app).

**Tests:** `AppsRepository` with a fake `LauncherApps` (list, sort, install/uninstall updates, extra profiles); clock Flow (minute alignment, time-zone change); Robolectric UI test (home → swipe up → list → tap fires launch); screenshots of home and list; logger (format, levels, rotation at 1 MB, 7-day cleanup, background writing, failure swallowed, forbidden content not logged), crash handler (writes file with last 50 entries, then delegates), exit-reason mapping with fakes, diagnostics screen UI + export intent; safe-mode trigger logic (2 crashes → normal, 3 within 60 s → safe, 3 spread out → normal, reset after 60 s stable and on exit), safe-mode screen UI with no DataStore access.

**Done when:** tests pass; manual check: ZeroOS can be set as default launcher on the phone, shows the clock, swipe up lists apps, tapping opens them, new installs appear without restart; a forced test crash (debug-only button) produces a crash report visible on the next start, and logs can be exported; a debug-only "crash on start" switch triggers safe mode after 3 starts, and "Open Home settings" lets me switch to the stock launcher.

---

## Phase 2 — v1.0 Features (F1–F6, F13, F14)

### Step 2.0 — Wireframes for Phase 2 ✅
App context menu, rename dialog, favorites edit mode, hidden apps, all Settings screens (home, app list, gestures, appearance, backup, diagnostics, about), onboarding pages. **Approved by me before Step 2.1.**

### Step 2.1 — Settings storage ✅
- Add DataStore. `core/data/SettingsRepository` on a typed DataStore saved as **JSON** with kotlinx-serialization (D-029).
- Stores favorites (ordered keys), hidden set, renames map, gesture bindings, appearance, onboarding flag.
- Default values come from REQUIREMENTS.md **Appendix A**.

### Step 2.2 — Design system (`core/ui`) ✅
- Theme tokens: pure black / light / system; monochrome palette; optional accent.
- Typography: **Doto** (OFL dot-matrix) for **all** text (F6.3, D-020); no font picker; size scale S/M/L/XL × system font scale with body text ≥ 14 sp. Screenshot tests check readability at default and largest sizes.
- Shared composables: `AppLabel`, `MinimalListItem`, `MinimalBottomSheet`, `SettingRow`, `ToggleRow`.
- Edge-to-edge with insets for gesture and 3-button navigation.

### Step 2.3 — Home screen (F1) ✅
- Clock tap → `AlarmClock.ACTION_SHOW_ALARMS`; date tap → `CATEGORY_APP_CALENDAR`; battery % from the `ACTION_BATTERY_CHANGED` sticky broadcast as a Flow.
- Favorites joined with apps (missing apps dropped → F3.4).
- Alignment; no scroll; long-press empty area → Settings; optional next alarm.
- Build the home layout from the final wireframe with **slots** for later elements (stats, focus button/timer, widgets) that take no space while empty, so Phase 4–5 only fill slots and don't rework the layout.
- Screenshot test: home at the largest font with all slots filled by placeholder content still fits without scrolling.

### Step 2.4 — App list & search (F2) ✅
- Search with auto-focus + keyboard (toggle); substring + initials + accent-insensitive matching; auto-launch single match (toggle).
- No-match actions: web search, Play Store search.
- Optional fast-scroller; profile tags (F2.10); back/swipe down → home.
- Precompute normalized labels/initials once per list change; `LazyColumn` with stable keys.
- JVM micro-benchmark test: matching 300 apps ≤ 16 ms per query (NFR-P3).

### Step 2.5 — Favorites (F3) ✅
- Add/remove from long-press menu and Settings; drag-and-drop edit mode; max count.

### Step 2.6 — Hide & rename (F5) ✅
- Long-press sheet: Add to favorites · Rename · Hide · App info · Uninstall.
- Rename dialog with reset; Settings → Hidden apps with unhide and optional confirmation/delay.

### Step 2.7 — Gestures (F4) ✅
- `pointerInput` swipe detection with thresholds from Appendix A.
- Swipe left/right/down bindings: app / action / none.
- Swipe down → search (default). The "notification shade" option is added in Step 4.2 together with the Accessibility Service, and falls back to search when the service is off. No hidden APIs (e.g., `StatusBarManager` reflection).
- Double-tap lock: shown as "needs Accessibility (v1.1)" until Phase 4.

### Step 2.8 — Appearance (F6) ✅
- Theme, wallpaper (solid / system with dim overlay), font, text size, visibility toggles.

### Step 2.9 — Onboarding (F14) ✅
- Intro (3–4 pages) → set as default (`RoleManager` / home settings) → pick favorites → background reliability (F14.6). Skippable; shown once.
- The set-as-default page says: keep the stock launcher installed and enabled as a fallback (F18.7).

### Step 2.10 — Backup & restore (F13) ✅
- Export/import JSON via Storage Access Framework (`CreateDocument`/`OpenDocument`); schema version field; import confirmation; Auto Backup rules for DataStore (and Room once it exists).
- From now on, every step that adds settings extends the backup format (F13.4).

### Step 2.11 — Settings structure ✅
Home · App list · Gestures · Appearance · Hidden apps · Backup · Diagnostics · About. Later phases add sections. Every setting change is logged (F17.7), with free-text values redacted (F17.8).

**Tests (written within each step):**
- Unit: SettingsRepository (defaults from Appendix A, read/write), search matching + benchmark, favorites rules, hide/rename/reset, gesture resolution, appearance mapping, onboarding state machine, battery-optimization detection logic (fake `PowerManager`), backup round-trip (export → import gives identical settings) and older-schema import.
- UI (Robolectric): home & alignment, search + no-match actions, long-press actions, favorites edit mode, rename/hide flows, gestures, onboarding flow & skip, backup export/import flow.
- Screenshots: every screen, dark/light, default/largest font.
- Process death / config change: ViewModel state restored via `SavedStateHandle` (NFR-R7).

**Done when:** every F1–F6, F13 and F14 requirement is met and traced to tests; manual check on the phone passes for each step, including export → uninstall → reinstall → import restoring all settings.

---

## Phase 3 — Daily-Driver Hardening (v1.0)

### Step 3.1 — Test audit ✅
- Every F1–F6/F13/F14 ID in TRACEABILITY.md has at least one test; fill gaps.

### Step 3.2 — Manual performance & stability check on the phone
- Home appears instantly after Home press (NFR-P1); cold start feels ≤ 0.5 s (NFR-P2).
- Memory in system settings → app info ≤ 60 MB idle (NFR-P4); battery usage of ZeroOS negligible after 24 h (NFR-P5).
- No LeakCanary reports or StrictMode violations in a debug build during 1 day of use.
- Rotation, dark-mode switch and font-size change keep state (NFR-R7).

### Step 3.3 — Upgrade check
- Install the previous APK, configure favorites/renames/hidden apps, install the new APK over it → all settings retained (NFR-R6).

### Step 3.4 — One week of daily use
- Use v1.0 as the default launcher for 7 days; file every bug as a regression test + fix.

**Done when:** audit complete; manual checks pass; one week of use with no open crash bugs.

---

## Phase 4 — v1.1 Wellbeing & Insights (F8, F9, F10; F7 deferred, D-039)

### Step 4.0 — Wireframes for Phase 4 ✅
Permission explanation screens, mindful pause, limit reached, wellbeing settings (F8 toggles), notification inbox and filter settings, focus picker, Insights, and the home screen-time stats in each style. **Approved by me before Step 4.1.**

### Step 4.1 — Infrastructure ✅
- ~~Add **Room** (limits, inbox, usage cache, focus schedules)~~: per-app rules and focus schedules live in the settings file instead (D-041); Room only if a later step needs it, with exported schemas and migration tests then.
- **WorkManager** arrives with scheduled focus in Step 4.5 (no midnight reset needed: limits count from midnight; digests deferred with F7) (D-040).
- New module `feature/wellbeing/` with its first screen in Step 4.3 (`feature/notifications/` deferred, D-039).
- **Permission helper**: one "explain → open system screen → verify on return → fallback" flow for Usage Access, Notification Access, Accessibility, DND. It includes the **restricted settings** instructions (App info → ⋮ → Allow restricted settings) for Accessibility/Notification access (F14.4, NFR-S4).

### Step 4.2 — Shared Accessibility Service ✅
- Single `ZeroAccessibilityService` for F4.4 (swipe down → notification shade via `GLOBAL_ACTION_NOTIFICATIONS`), F4.5 (double-tap lock via `GLOBAL_ACTION_LOCK_SCREEN`, API 28+), F8.7, and later F16.
- Off by default; `serviceInfo.packageNames` updated at runtime to only the packages enabled features need.
- Detect disabled/killed state and show a warning in Settings.

### Step 4.3 — Usage limits & mindful pause (F8)
Build in this order:
1. **Toggles (F8.8–F8.12)**: master (off by default) + per-part switches; config kept when off; permissions on first enable; background work stopped on disable; disable friction.
2. **Mindful pause (F8.1, F8.2)**: intercept launches from the launcher for flagged apps → full-screen pause with countdown + optional intention field.
3. **Daily limits (F8.3–F8.6)**: today's usage via `UsageStatsManager.queryEvents` since local midnight; cache in Room; block screen with "5 more minutes"; remaining time in app list.
4. **Outside-launcher enforcement (F8.7)**: Accessibility service on `TYPE_WINDOW_STATE_CHANGED` for limited packages → block overlay.
5. **Take a break (F8.16–F8.18, D-044)**: same service; one timer per foreground session of a chosen app → break overlay with list D/E puns.

### Step 4.4 — Notification filter (F7) — deferred (D-039)
*Not built in v1.1; kept here for later together with wireframe H9. Skip the inbox table, `feature/notifications/` and the digest work in Step 4.1 until then.*
- `NotificationListenerService`: allowed apps pass; others → `cancelNotification` + store in Room inbox.
- Never filter `CATEGORY_CALL`, `CATEGORY_ALARM`, or system/critical notifications.
- Inbox screen (open stored `contentIntent` if valid, dismiss, clear all); scheduled digests.

### Step 4.5 — Focus mode (F9) ✅
- Start from the home **"focus" text button** (fills the focus slot from Step 2.3) or Settings → duration + allow-list; while active the button shows remaining time and hold-3s ends it; filtered app list, maximal notification filtering, remaining time on home, hold-3s to end; optional DND; scheduled sessions.
- Focus icon (F9.8–F9.10, D-047): four dot designs from `docs/brand/focus-marks/`, picked in Settings → Wellbeing → Focus; shown on the picker, a 2 s "Focus started" screen, home and the settings row. Focus works with Wellbeing off.

### Step 4.6 — Screen-time insights & home-screen stats (F10) ✅
Reuses the usage calculation from Step 4.3.
- `UsageSummaryRepository`: today's screen time, unlocks (`KEYGUARD_HIDDEN`, Android 9+), per-hour buckets, last 7 days, yesterday comparison.
- Insights screen: totals, top 5 apps, 7-day monochrome bars.
- Home-screen stats in `feature/home`: four styles (text, progress bar, weekly bars, hourly timeline) drawn with Compose `Canvas`; goal and yesterday comparison options; refresh on home visible / screen on; tap → Insights; "Tap to show screen time" line until Usage Access is granted.
- Must fit the non-scrolling home layout (F1.6) at the largest font scale.

**Tests:**
- Unit: F8 toggles (master off disables all, per-part, config kept, friction), usage calculation from fake `UsageEvents` (midnight, time-zone, DST, clock change, "5 more minutes" cap), notification filter rules, digest scheduling, focus timer & allow-list, permission helper states (granted/denied/revoked), backup round-trip for all new F7–F10 settings (F13.4), F10 aggregation (screen time, unlocks, hourly buckets, 7-day, yesterday %, goal exceeded, Android 8 no-unlock fallback), chart data mapping and content descriptions.
- Robolectric: Room DAOs + migration tests, WorkManager jobs (`work-testing`), notification listener logic with fake `StatusBarNotification`s, accessibility event handling with fake events.
- UI + screenshots: pause, limit-reached, inbox, focus start/end, permission explanation screens, Insights screen, home with each stats style (dark/light, default/largest font, permission not granted).

**Done when:** F8–F10 met and traced (F7 deferred); every feature works with permissions denied; manual check: limits work on the phone, services survive overnight + reboot with the F14.6 settings applied, and home-screen screen time/unlocks roughly match the phone's own screen-time screen.

---

## Phase 5 — v1.2 Extras (F11, F12) — deferred to the end (D-056)
Draft wireframes are in section I of docs/wireframes.md, not yet approved. Open questions for then: to-do storage
(settings file vs Room) and whether done tasks disappear at once.

### Step 5.0 — Wireframes for Phase 5
Grayscale screen, widget settings, and home calendar/to-do lines. **Approved by me before Step 5.1.**

### Step 5.1 — Grayscale shortcut (F11)
- Settings entry opening the system accessibility/color-correction screen + accessibility-shortcut tip. No `WRITE_SECURE_SETTINGS`.

### Step 5.2 — Minimal widgets (F12)
- Next calendar event (`READ_CALENDAR`) and 3-item to-do in Room. All off by default. No weather.

**Tests:**
- Unit: calendar mapping, to-do rules, backup round-trip including to-do items (F13.4).
- UI + screenshots: widgets, grayscale screen.
- Permission allowlist updated for `READ_CALENDAR` with its ID.

**Done when:** F11–F12 met and traced; manual check on the phone.

---

## Phase 6 — v1.3 Short-Form Content Blocker (F16)

### Step 6.0 — Wireframes for Phase 6 ✅
Block overlay, short-form blocker settings, explanation screen. **Approved by me before Step 6.1.**

### Step 6.1 — Detection rules as data
- `assets/shortform_rules.json`: `{ version, apps: [{ package, viewIds: [...], minAppVersion? }] }`.
- View IDs taken from the node-tree dumps captured with the debug dumper (Step 0.5); re-capture if apps have updated.

### Step 6.2 — Detection in the shared Accessibility Service
- Package filter limited to enabled target apps; throttle per Appendix A; `findAccessibilityNodeInfosByViewId`; no full-tree traversal.

### Step 6.3 — Block overlay & limit mode
- `TYPE_ACCESSIBILITY_OVERLAY` black screen with a random pun (or custom message) + "Go home" (`GLOBAL_ACTION_HOME`); optional auto-home; no grace period beyond a ~1 s transition pause (F16.9, F16.10).
- Limit mode with daily allowance and midnight reset; "Allow 5 minutes" via the pause screen; always blocks during Focus; stats into Insights.
- Explanation screen with restricted-settings instructions (F16.2); killed-service warning (F16.15).

**Tests:**
- Unit: backup round-trip for F16 settings (F13.4), rules parsing & versioning (valid/invalid/unknown version), detection against **recorded node-tree fixtures** per app (Shorts → detected; normal video/feed → not detected), throttling, limit accumulation (midnight/time-zone/DST), focus override, bypass cap.
- Robolectric: service event handling with fake events; overlay show/dismiss logic.
- Screenshots: overlay, settings, explanation screen.

**Done when:** F16 met and traced; manual check on the phone: Shorts/Reels blocked within ~300 ms, normal videos untouched, battery impact acceptable after 1 day.

---

## Install Checklist (every time a new APK goes on the phone)
1. `./gradlew check` passes.
2. Bump `versionCode`; update `versionName` if a milestone changed.
3. Move "Unreleased" entries in CHANGELOG.md under the new version.
4. Export a settings backup on the phone (F13).
5. `./gradlew assembleRelease` (signed with the backed-up keystore).
6. Install over the existing app; confirm settings retained. From Phase 4 on: pause Play Protect scanning before installing and **turn it back on right after** (D-018).
7. Quick smoke test: Home, search, launch, gestures, plus any enabled wellbeing/F16 features.
8. If something is broken: rebuild the last good commit with a higher `versionCode` and install it (keep the last 2 APKs in a local `releases/` folder, git-ignored, for reference).
9. Before installing a debug APK over the everyday app, export a settings backup (F13) in case the debug build breaks settings.

---

## Ongoing
- When YouTube/Instagram update and F16 breaks: capture new node-tree dumps, update fixtures and rules, rebuild.
- Bump target SDK when a new Android version reaches the phone.
- Every bug: regression test first.

## Deferred Work (not planned now; revisit when wanted)
- Notification filter and inbox (F7, Step 4.4, wireframe H9; D-039).
- Phase 5: grayscale shortcut (F11), calendar line and to-do (F12) (D-056).
- Scheduled focus sessions (F9.6, wireframe H10a; editor H10c to be approved; WorkManager for Do Not Disturb at the exact times) (D-054).
- Instrumented tests on an emulator or the phone.
- Macrobenchmark + Baseline Profiles for startup (NFR-P1/P2 measured automatically).
- Translations / RTL.
- ADB-based grayscale toggle.
- Accent color (F6.6, removed by D-035).
