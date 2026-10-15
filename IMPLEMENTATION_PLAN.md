# ZeroOS Launcher — Implementation Plan

Step-by-step build plan for the features in [REQUIREMENTS.md](REQUIREMENTS.md).
Each step lists **what to build**, **key technical notes**, and a **done when** checklist. Steps are ordered so the app is runnable and usable after every phase.

Time estimates assume **one developer, full-time**. Scale accordingly.

| Phase | Scope | Est. time | Outcome |
|---|---|---|---|
| 0 | Project setup | 3–4 days | Empty app builds, CI green |
| 1 | Prototype (M0) | 1 week | Usable as default launcher |
| 2 | MVP features | 3–4 weeks | F1–F6, F14 complete |
| 3 | Hardening & release v1.0 | 2 weeks | On Play Store |
| 4 | v1.1 Wellbeing | 4–5 weeks | F7, F8, F9 |
| 5 | v1.2 Insights & Premium | 4 weeks | F10–F13, F15 |
| 6 | v1.3 Short-form blocker | 3–5 weeks | F16 |
| **Total** | | **~5 months** | |

---

## Phase 0 — Project Setup

### Step 0.1 — Tooling
- Install latest stable Android Studio, JDK 17+, Android SDK (latest stable target SDK, min SDK 26).
- Prepare test devices: one Pixel (stock Android), one Samsung; add a Xiaomi/OnePlus later for OEM testing.

### Step 0.2 — Create the project
- New project → *Empty Activity (Compose)*, package `com.zeroos.launcher`.
- Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`).
- Add dependencies: Compose BOM, Material 3, Navigation Compose, Hilt, DataStore, Room, Coroutines, WorkManager, Lifecycle.
- Enable R8/minify for release builds.

### Step 0.3 — Module structure
Start with modules you need now; add feature modules when their phase begins.
```
app/
core/ui/
core/data/
core/model/
feature/home/
feature/applist/
feature/settings/
feature/onboarding/
```

### Step 0.4 — Quality gates
- ktlint/detekt, Android Lint with warnings as errors on CI.
- GitHub Actions: build, lint, unit tests on every push/PR.
- `.gitignore`, `README.md`, `LICENSE`.

**Done when:** `./gradlew assembleDebug test lint` passes locally and on CI.

---

## Phase 1 — Prototype (M0): A Working Launcher

### Step 1.1 — Register as a home app
In `AndroidManifest.xml`, declare `MainActivity` with:
- intent filter: `MAIN` + `HOME` + `DEFAULT` categories
- `launchMode="singleTask"`, `excludeFromRecents="true"`, `stateNotNeeded="true"`, `clearTaskOnLaunch="true"`
- `<queries>` block with a `MAIN`/`LAUNCHER` intent (package visibility on Android 11+).

Behaviour:
- Pressing Home while already on the launcher → `onNewIntent` closes the drawer and returns to the home page.
- Back button on home → does nothing.

### Step 1.2 — Load installed apps
- `core/model`: `AppEntry(packageName, activityName, user: UserHandle, label, customLabel?, isHidden)`; unique key = component + user serial.
- `core/data/AppsRepository`:
  - Use `LauncherApps.getActivityList(null, user)` for every profile in `UserManager.userProfiles`.
  - Register `LauncherApps.Callback` → expose `Flow<List<AppEntry>>` that updates on install/uninstall/update.
  - Sort labels with a locale-aware `Collator`.
- Launch apps with `LauncherApps.startMainActivity(component, user, ...)`.

### Step 1.3 — Bare-bones UI
- Home: big clock + date (static composables, ticking via a `Flow` that emits every minute aligned to the minute boundary).
- Swipe up → simple scrollable list of all app labels; tap launches.

**Done when:** you can set ZeroOS as default launcher on a real device, see the clock, swipe up, and open any app; new installs appear without restarting.

---

## Phase 2 — MVP Features (F1–F6, F14)

### Step 2.1 — Settings storage (foundation)
- `core/data/SettingsRepository` on **DataStore** (Proto DataStore recommended for typed settings).
- Store: favorites (ordered list of keys), hidden set, renames map, gesture bindings, appearance settings, onboarding-completed flag.
- Room is **not needed yet**; add it in Phase 4.

### Step 2.2 — Design system (`core/ui`)
- Theme tokens: pure black / light / system; monochrome palette; optional accent.
- Typography: bundle 3–5 fonts (OFL-licensed, e.g., Inter, JetBrains Mono, Lora); text size scale S/M/L/XL multiplied by system font scale.
- Shared composables: `AppLabel`, `MinimalListItem`, `MinimalBottomSheet`, `SettingRow`, `ToggleRow`.
- Edge-to-edge with proper insets for gesture and 3-button navigation.

### Step 2.3 — Home screen (F1)
- Clock (tap → `AlarmClock.ACTION_SHOW_ALARMS`), date (tap → calendar app via `CATEGORY_APP_CALENDAR`), battery % (`ACTION_BATTERY_CHANGED` sticky broadcast as a Flow).
- Favorites list from SettingsRepository joined with AppsRepository (drop missing apps automatically → F3.4).
- Alignment left/center/right; no scrolling; long-press empty area → Settings.
- Optional next alarm (`AlarmManager.nextAlarmClock`) line.

### Step 2.4 — App list & search (F2)
- Search field with auto-focus + keyboard (toggle).
- Matching: case-insensitive substring + initials ("gm" → Google Maps); normalize accents.
- Auto-launch on single match (toggle).
- No-match actions: web search intent, Play Store search intent.
- Optional alphabet fast-scroller.
- Work-profile apps tagged "· work".
- Back / swipe down → home.
- Use `LazyColumn` with stable keys; precompute lowercase/initials once per app list change.

### Step 2.5 — Favorites management (F3)
- Add/remove from app-list long-press menu and from Settings.
- Edit mode with drag-and-drop reorder; max count configurable (default 6, max 10).

### Step 2.6 — Hide & rename (F5)
- Long-press bottom sheet: Add to favorites · Rename · Hide · App info · Uninstall.
  - App info: `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` (or `LauncherApps.startAppDetailsActivity`).
  - Uninstall: `Intent.ACTION_DELETE` with `package:` URI.
- Rename dialog with "Reset to original".
- Settings → Hidden apps: list with unhide; optional delay/confirmation before showing.

### Step 2.7 — Gestures (F4)
- Detect swipes on the home root with `pointerInput` (thresholds in dp, velocity-aware).
- Configurable: swipe left/right/down → app / action / none.
- Swipe down → expand notification shade (via reflection-free approach: Accessibility `GLOBAL_ACTION_NOTIFICATIONS` if enabled, otherwise open search).
- Double-tap to lock: implement in Phase 4 together with the shared Accessibility Service (show "requires permission" for now).

### Step 2.8 — Appearance (F6)
- Settings screens for theme, wallpaper (solid color, or system wallpaper with dim overlay using `windowShowWallpaper`), font, text size, show/hide clock/date/battery/status bar.

### Step 2.9 — Onboarding (F14)
- 3–4 pages explaining the idea → "Set as default" (`RoleManager.createRequestRoleIntent(ROLE_HOME)` on API 29+, otherwise open `Settings.ACTION_HOME_SETTINGS`) → pick favorites (suggest Phone, Messages, Camera, Maps).
- Skippable; shown once.

### Step 2.10 — Settings screen structure
Home · App list · Gestures · Appearance · Hidden apps · About. (Wellbeing, Notifications, Backup, Premium sections added in later phases.)

**Done when:** every F1–F6 and F14 requirement in REQUIREMENTS.md is met and manually verified on 2 devices.

---

## Phase 3 — Hardening & v1.0 Release

### Step 3.1 — Testing
- Unit tests: search matching, favorites ordering, settings serialization, app-list merging.
- Compose UI tests: home renders, swipe up opens list, search launches app, rename/hide flows.
- Macrobenchmark + Baseline Profile: verify NFR-P1 (home ≤ 100 ms warm) and NFR-P2 (cold ≤ 500 ms).

### Step 3.2 — Accessibility & localization
- TalkBack pass, 48dp targets, contrast check (NFR-A1–A4).
- All strings in `strings.xml`; test RTL with pseudo-locales.

### Step 3.3 — Device matrix
- Pixel, Samsung, Xiaomi, OnePlus; gesture & 3-button nav; work profile; secondary user; tablet/foldable layout sanity.

### Step 3.4 — Release prep
- App icon, Play listing text & screenshots, privacy policy page.
- Signing (Play App Signing), versioning scheme, release build with R8.
- Internal testing track → closed testing (≥ 12 testers for 14 days if a new personal developer account) → production.
- Opt-in crash reporting only (or none) per NFR-S1.

**Done when:** v1.0 is live on Play Store; crash-free rate is being monitored.

---

## Phase 4 — v1.1 Wellbeing (F7, F8, F9)

### Step 4.1 — Infrastructure
- Add **Room** to `core/data` (tables: app limits, notification inbox, usage cache, focus schedules).
- Add **WorkManager** jobs: midnight reset, notification digests, scheduled focus.
- New modules: `feature/notifications/`, `feature/wellbeing/`.
- **Permission helper**: one reusable "explain → request → verify → fallback" flow for Usage Access, Notification Access, Accessibility, DND access (F14.4, NFR-S4).

### Step 4.2 — Shared Accessibility Service
- Single `ZeroAccessibilityService` used by F4.5 (double-tap lock via `GLOBAL_ACTION_LOCK_SCREEN`, API 28+), F8.7, and later F16.
- Opt-in only, with prominent disclosure + consent screen; start minimal (no package filter needed until F8.7/F16 are enabled — update `serviceInfo` at runtime based on enabled features).
- Detect disabled/killed state and show a warning in Settings.

### Step 4.3 — Usage limits & mindful pause (F8)
Build in this order:
1. **Toggles first (F8.8–F8.12):** master switch (off by default) + per-part switches stored in DataStore; config preserved when off; permissions requested on first enable; background work stopped on disable; optional "hold to disable" friction.
2. **Mindful pause (F8.1, F8.2):** intercept launches *from the launcher* for flagged apps → full-screen pause activity/composable with countdown and optional intention field.
3. **Daily limits (F8.3–F8.6):** compute today's usage per app via `UsageStatsManager.queryEvents` (foreground/background events since local midnight); cache in Room; block screen with "5 more minutes" (max N/day); remaining time label in app list.
4. **Outside-launcher enforcement (F8.7):** Accessibility service watches `TYPE_WINDOW_STATE_CHANGED` for limited packages → show block overlay.

### Step 4.4 — Notification filter (F7)
- `NotificationListenerService`: allowed apps pass through; others → `cancelNotification` + save to Room inbox.
- Never filter calls, alarms, system/critical categories (`CATEGORY_CALL`, `CATEGORY_ALARM`, etc.).
- Inbox screen (open via stored `contentIntent` when still valid, dismiss, clear all).
- Scheduled digest notifications via WorkManager.

### Step 4.5 — Focus mode (F9)
- Start from home (long-press clock) → duration + allow-list.
- While active: app list filtered to allowed apps, maximal notification filtering, remaining time on home, hold-3s to end early.
- Optional DND via `NotificationManager.setInterruptionFilter` (needs DND access).
- Scheduled sessions via WorkManager / exact alarms.

**Done when:** F7–F9 requirements pass; all features work with permissions denied (graceful fallback); battery stays within NFR-P5 on a 24h idle test.

---

## Phase 5 — v1.2 Insights, Extras & Premium (F10–F13, F15)

### Step 5.1 — Screen-time insights (F10)
- Daily total, unlock count (`KEYGUARD_HIDDEN` / `SCREEN_INTERACTIVE` usage events), top 5 apps, 7-day monochrome bars; optional home-screen line.

### Step 5.2 — Grayscale (F11)
- Guide screen with per-OEM instructions; if `WRITE_SECURE_SETTINGS` granted via ADB, toggle `accessibility_display_daltonizer` settings + schedule.

### Step 5.3 — Minimal widgets (F12)
- Weather (coarse location, opt-in; a free weather API; cache 1–3 h), next calendar event (`CalendarContract`, READ_CALENDAR), 3-item to-do in Room. All off by default.

### Step 5.4 — Backup & restore (F13)
- Export/import JSON via Storage Access Framework (`CreateDocument`/`OpenDocument`); schema version field; Auto Backup rules for DataStore files.

### Step 5.5 — Premium (F15)
- `feature/billing/` with Play Billing Library: products, purchase flow, acknowledgement, restore, 7-day trial.
- `PremiumGate` in `core` — single place to check entitlements; premium features show an upsell instead of hiding.

**Done when:** F10–F13 and F15 requirements pass; billing tested with license testers.

---

## Phase 6 — v1.3 Short-Form Content Blocker (F16)

### Step 6.1 — Detection rules as data
- `rules.json` bundled in assets: `{ version, apps: [{ package, viewIds: [...], minAppVersion? }] }`.
- Start with YouTube Shorts and Instagram Reels; find view IDs with Android Studio Layout Inspector / `uiautomator dump`.

### Step 6.2 — Detection in the shared Accessibility Service
- Enable package filter for selected target apps only.
- On window-state/content events, throttle to ≤ 1 check per 300–500 ms, use `findAccessibilityNodeInfosByViewId`; recycle nodes.

### Step 6.3 — Block overlay & limit mode
- `TYPE_ACCESSIBILITY_OVERLAY` black screen with custom message + "Go back" (`GLOBAL_ACTION_BACK`).
- Limit mode: accumulate time while detected → block after daily allowance; reset at midnight; "Allow 5 minutes" through mindful pause.
- Always block during Focus mode; report stats into Insights.

### Step 6.4 — Remote rule updates
- WorkManager job ≤ 1×/day fetching a **signed** rules file (verify signature with a bundled public key); fall back to bundled rules.

### Step 6.5 — Policy & release
- Prominent disclosure, Play Accessibility declaration form, reviewer demo video, `isAccessibilityTool=false`.
- Build flag to strip F16 if the Play review fails.

**Done when:** F16 requirements pass; performance budget met (≤ 2% battery/day, ≤ 300 ms overlay latency); Play review approved.

---

## Ongoing (after every release)
- Monitor crash rate, ANRs, and reviews; triage weekly.
- Re-verify F16 detection rules whenever YouTube/Instagram update.
- Keep target SDK current (Play requires updating yearly).
- Re-run Macrobenchmarks before each release to catch performance regressions.
