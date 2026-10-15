# ZeroOS Launcher — Requirements & Features

> A minimalist Android home-screen launcher that replaces colorful icons and endless feeds with a calm, text-first interface, helping people use their phone intentionally instead of compulsively.

| | |
|---|---|
| **Platform** | Android (min SDK 26 / Android 8.0, target latest stable SDK) |
| **Type** | Home-screen launcher (`android.intent.category.HOME`) |
| **Status** | Draft v0.1 |
| **Last updated** | 2026-10-15 |

---

## 1. Vision & Goals

### 1.1 Problem
Default launchers are designed for engagement: bright icons, notification badges, and widgets pull users into apps they didn't intend to open. Users want to reduce screen time without giving up a smartphone.

### 1.2 Goals
1. **Reduce mindless phone use** — make opening an app a deliberate act.
2. **Remove visual noise** — no icons, no badges, no colors by default.
3. **Stay fast and functional** — every essential app is ≤ 2 interactions away.
4. **Respect privacy** — no tracking, no ads, all data stays on the device.

### 1.3 Non-Goals
- Not a full OS or ROM replacement.
- Not a parental-control or MDM tool (no remote enforcement).
- No icon packs, themes marketplace, or heavy widget ecosystem.

### 1.4 Target Users
| Persona | Need |
|---|---|
| **The Distracted Professional** | Wants fewer interruptions during work hours. |
| **The Digital Minimalist** | Wants a calm, aesthetic, text-only phone. |
| **The Student** | Wants to limit social media and gaming during study time. |
| **The Recovering Doomscroller** | Wants friction before opening addictive apps. |

---

## 2. Feature Overview

| ID | Feature | Priority | Release |
|---|---|---|---|
| F1 | Minimal home screen (clock, date, favorites) | P0 | MVP |
| F2 | Text-based app list with search | P0 | MVP |
| F3 | Favorite apps management | P0 | MVP |
| F4 | Gestures & shortcuts | P0 | MVP |
| F5 | Hide & rename apps | P0 | MVP |
| F6 | Appearance customization | P1 | MVP |
| F7 | Notification filter / inbox | P1 | v1.1 |
| F8 | App usage limits & mindful pause | P1 | v1.1 |
| F9 | Focus mode | P1 | v1.1 |
| F10 | Screen-time insights | P2 | v1.2 |
| F11 | Grayscale mode | P2 | v1.2 |
| F12 | Minimal widgets (weather, calendar, to-do) | P2 | v1.2 |
| F13 | Backup & restore settings | P2 | v1.2 |
| F14 | Onboarding | P0 | MVP |
| F15 | Premium / monetization | P2 | v1.2 |
| F16 | Short-form content blocker (Reels, Shorts, TikTok) | P2 | v1.3 |

Priority: **P0** = must have, **P1** = should have, **P2** = nice to have.

---

## 3. Functional Requirements

### F1 — Minimal Home Screen
The default screen shown when the user presses Home.

| ID | Requirement |
|---|---|
| F1.1 | Display current **time** in large text; tapping it opens the default Clock/Alarm app. |
| F1.2 | Display **date** (e.g., "Thursday, 15 October"); tapping it opens the default Calendar app. |
| F1.3 | Optionally display **battery percentage** as plain text. |
| F1.4 | Display a vertical list of **0–10 favorite apps** as text labels (no icons). |
| F1.5 | No notification badges, dots, or counters on the home screen. |
| F1.6 | Home screen must not scroll; all content fits within one viewport. |
| F1.7 | Support configurable alignment of the favorites list: left, center, right. |
| F1.8 | Optional: show the next upcoming calendar event or alarm in small text. |
| F1.9 | Long-press on empty space opens launcher settings. |

### F2 — App List (Drawer)

| ID | Requirement |
|---|---|
| F2.1 | Open via swipe up (configurable) from the home screen. |
| F2.2 | Show all installed, launchable, non-hidden apps as an **alphabetical text list**. |
| F2.3 | Search bar at top, **keyboard auto-opens** on entry (toggleable). |
| F2.4 | Search is case-insensitive, matches substrings and initials (e.g., "gm" → "Google Maps"). |
| F2.5 | If search yields exactly one match, optionally **auto-launch** it. |
| F2.6 | If no match, offer "Search the web for '<query>'" and "Search Play Store". |
| F2.7 | Optional alphabetical fast-scroller on the side. |
| F2.8 | Long-press an app → context menu: *Add to favorites, Rename, Hide, Set limit, App info, Uninstall*. |
| F2.9 | List updates live when apps are installed/uninstalled/updated (`PACKAGE_ADDED/REMOVED/CHANGED`). |
| F2.10 | Support work-profile apps, visually tagged (e.g., "Gmail · work"). |
| F2.11 | Back gesture or swipe down returns to home. |

### F3 — Favorite Apps

| ID | Requirement |
|---|---|
| F3.1 | User can add/remove favorites from the app list or settings. |
| F3.2 | User can reorder favorites via drag-and-drop in an edit mode. |
| F3.3 | Max favorites configurable (default 6, max 10). |
| F3.4 | If a favorite app is uninstalled, it is removed automatically. |

### F4 — Gestures & Shortcuts

| ID | Requirement |
|---|---|
| F4.1 | **Swipe up** → app list (default). |
| F4.2 | **Swipe left** → user-assigned app (default: Camera). |
| F4.3 | **Swipe right** → user-assigned app (default: Phone/Dialer). |
| F4.4 | **Swipe down** → expand notification shade or open search (configurable). |
| F4.5 | **Double-tap** → lock screen (requires Accessibility Service or Device Admin; must be opt-in with clear explanation). |
| F4.6 | Each gesture can be set to: an app, a launcher action, or "None". |

### F5 — Hide & Rename Apps

| ID | Requirement |
|---|---|
| F5.1 | Hide any app from the app list and search. |
| F5.2 | Hidden apps are viewable/restorable in Settings → Hidden apps. |
| F5.3 | Optionally require a confirmation or delay to open the hidden apps list. |
| F5.4 | Rename any app label (e.g., "Instagram" → "Time Sink"); rename persists across updates. |
| F5.5 | Reset to original name option. |

### F6 — Appearance

| ID | Requirement |
|---|---|
| F6.1 | Themes: **Dark** (default, pure black for OLED), **Light**, **System**. |
| F6.2 | Wallpaper: solid color (default black), or system wallpaper with dim overlay. |
| F6.3 | Font: choice of 3–5 bundled fonts + system font. |
| F6.4 | Text size: small / medium / large / extra-large; respects system font scale. |
| F6.5 | Toggle visibility of clock, date, battery, status bar. |
| F6.6 | Accent color is monochrome by default; optional single accent color. |

### F7 — Notification Filter

| ID | Requirement |
|---|---|
| F7.1 | Uses `NotificationListenerService` (opt-in, with explanation). |
| F7.2 | User selects **allowed apps** whose notifications come through normally. |
| F7.3 | Notifications from other apps are silenced and collected into a **Notification Inbox** inside the launcher. |
| F7.4 | Optional **scheduled digest** (e.g., 12:00 and 18:00) showing a single summary notification. |
| F7.5 | Inbox items can be opened, dismissed, or cleared all. |
| F7.6 | Calls, alarms, and system-critical notifications are never filtered. |

### F8 — App Usage Limits & Mindful Pause

| ID | Requirement |
|---|---|
| F8.1 | **Mindful pause**: before opening a flagged app, show a full-screen breathing/countdown screen (3–10 s, configurable) with "Open anyway" / "Go back". |
| F8.2 | Optional prompt: "Why are you opening this?" with a short intention text field. |
| F8.3 | **Daily time limit** per app (e.g., 30 min); uses `UsageStatsManager` (requires Usage Access permission). |
| F8.4 | When limit is reached, show a blocking screen on launch from the launcher; allow "5 more minutes" (max N times/day, configurable). |
| F8.5 | Show remaining time for limited apps in the app list (small subtle text). |
| F8.6 | Limits reset at local midnight. |
| F8.7 | Enforcement outside the launcher (e.g., opening via notification) is **best-effort** using an optional Accessibility Service; must be clearly disclosed. |
| F8.8 | **Master on/off toggle** for the whole feature in Settings → Wellbeing; **off by default**. When off, no pause screens, limits, or remaining-time labels are shown and no usage tracking runs. |
| F8.9 | **Individual toggles** for each part: Mindful pause (F8.1), Intention prompt (F8.2), Daily limits (F8.3–F8.6), and Outside-launcher enforcement (F8.7). Each can be turned on/off independently while the master toggle is on. |
| F8.10 | Turning the feature off **keeps the user's configuration** (flagged apps, limits, pause duration) so it's restored when turned back on. |
| F8.11 | Permissions (Usage Access, Accessibility) are requested only when the related toggle is first switched on; switching off stops the related background work immediately. |
| F8.12 | Optional friction when turning off during an active limit (e.g., mindful pause or "hold for 3 s"), configurable, to prevent impulsive disabling. |

### F9 — Focus Mode

| ID | Requirement |
|---|---|
| F9.1 | One-tap Focus mode from home (long-press clock or settings). |
| F9.2 | User picks a duration (25 / 50 / 90 min / custom) and an allow-list of apps. |
| F9.3 | During focus: only allowed apps are shown/launchable; notification filter is maximal. |
| F9.4 | Home screen shows remaining focus time. |
| F9.5 | Ending early requires a deliberate action (e.g., hold for 3 s). |
| F9.6 | Optional scheduled focus sessions (e.g., weekdays 09:00–12:00). |
| F9.7 | Optionally toggle system Do Not Disturb (requires DND access). |

### F10 — Screen-Time Insights

| ID | Requirement |
|---|---|
| F10.1 | Daily total screen time and number of unlocks. |
| F10.2 | Top 5 apps by usage today, plus 7-day trend (simple text/bar, monochrome). |
| F10.3 | Optional subtle screen-time text on the home screen (e.g., "1h 12m today"). |
| F10.4 | All data computed locally from `UsageStatsManager`; nothing uploaded. |

### F11 — Grayscale Mode

| ID | Requirement |
|---|---|
| F11.1 | Guide the user to enable system grayscale (Developer options / Digital Wellbeing / Bedtime mode), since third-party apps cannot toggle it directly without ADB-granted `WRITE_SECURE_SETTINGS`. |
| F11.2 | If `WRITE_SECURE_SETTINGS` is granted via ADB, allow one-tap toggle and scheduling. |

### F12 — Minimal Widgets

| ID | Requirement |
|---|---|
| F12.1 | **Weather**: single text line (e.g., "18° Cloudy"); location coarse, opt-in. |
| F12.2 | **Calendar**: next event text line. |
| F12.3 | **To-do**: up to 3 short tasks, editable inline, stored locally. |
| F12.4 | All widgets are text-only and individually toggleable; off by default. |

### F13 — Backup & Restore

| ID | Requirement |
|---|---|
| F13.1 | Export all settings (favorites, hidden, renames, limits, gestures) to a JSON file. |
| F13.2 | Import from file with confirmation. |
| F13.3 | Support Android Auto Backup for settings. |

### F14 — Onboarding

| ID | Requirement |
|---|---|
| F14.1 | 3–4 screen intro explaining the philosophy. |
| F14.2 | Prompt to set ZeroOS as default launcher (`RoleManager.ROLE_HOME` on Android 10+). |
| F14.3 | Let user pick initial favorites (suggest Phone, Messages, Camera, Maps). |
| F14.4 | Permissions requested **just-in-time** when the related feature is enabled, never all upfront. |
| F14.5 | Onboarding is skippable. |

### F15 — Monetization (optional)

| ID | Requirement |
|---|---|
| F15.1 | Core launcher (F1–F6, F14) is **free forever**, with no ads. |
| F15.2 | Premium (one-time or subscription) unlocks: notification filter, usage limits, focus scheduling, insights, extra fonts, widgets, short-form content blocker (F16). |
| F15.3 | Billing via Google Play Billing Library. |
| F15.4 | Free trial (e.g., 7 days) for premium features. |

### F16 — Short-Form Content Blocker
Blocks or time-limits short-form video feeds (YouTube Shorts, Instagram Reels, TikTok, etc.) **inside** other apps, showing a minimal black screen with a custom message. The rest of each app stays usable.

**Approach:** Extends the optional Accessibility Service from F8.7. No official Android API exists for this, so detection relies on matching screen elements (view resource IDs) of target apps.

| ID | Requirement |
|---|---|
| F16.1 | Feature is **opt-in and off by default**; the launcher is fully functional if Accessibility permission is denied. |
| F16.2 | Before enabling, show a **prominent disclosure** screen explaining what is read (only on-screen structure of selected apps), why, and that nothing leaves the device; require explicit consent. |
| F16.3 | Supported apps at launch: **YouTube Shorts** and **Instagram Reels**. Later: TikTok, Facebook Reels, Snapchat Spotlight. |
| F16.4 | User can enable/disable blocking per supported app. |
| F16.5 | Service listens only to events from the selected target packages (`packageNames` filter in the service config). |
| F16.6 | Detection uses targeted lookups (`findAccessibilityNodeInfosByViewId`) on window-state/content-change events, **throttled to ≤ 1 check per 300–500 ms**; no full-tree traversal. |
| F16.7 | **Block mode:** on detection, immediately show a full-screen black overlay (`TYPE_ACCESSIBILITY_OVERLAY`) with the user's custom message and a "Go back" button. |
| F16.8 | **Limit mode:** track time spent in short-form content per day; show the block overlay once the daily allowance (e.g., 15 min) is used up. Allowance resets at local midnight. |
| F16.9 | Custom message is user-editable (max 120 chars); default: "Nothing to see here. Go do something real." |
| F16.10 | "Go back" performs `GLOBAL_ACTION_BACK` and dismisses the overlay; optionally auto-trigger back after N seconds. |
| F16.11 | Optional friction to bypass: "Allow 5 minutes" after a mindful pause (reuses F8.1), limited to N times/day. |
| F16.12 | Detection rules (package → view IDs) are stored as **versioned data**, not hard-coded, so they can be updated without changing logic. |
| F16.13 | Optional **remote rule updates**: fetch a signed rules JSON periodically (WorkManager, ≤ 1×/day); falls back to bundled rules. No user data is sent. |
| F16.14 | Show today's short-form time and block count in Insights (F10). |
| F16.15 | Detect and warn the user when the service has been killed or disabled by the system/OEM, with guidance to disable battery optimization. |
| F16.16 | Respect Focus mode (F9): during focus, short-form content is always blocked regardless of limits. |

**Performance budget (additional, on top of NFR-P):**
| Metric | Target |
|---|---|
| Extra battery drain | ≤ 2% per day with heavy target-app use |
| Extra memory | ≤ 20 MB |
| Overlay appearance latency after detection | ≤ 300 ms |
| APK size increase | ≤ 100 KB |

**Estimated effort:** ~3–5 weeks for one developer (2 apps), plus **ongoing maintenance** of detection rules whenever target apps update their UI.

---

## 4. Non-Functional Requirements

### 4.1 Performance
| ID | Requirement |
|---|---|
| NFR-P1 | Home screen renders within **≤ 100 ms** after pressing Home (warm). |
| NFR-P2 | Cold start ≤ 500 ms on a mid-range device. |
| NFR-P3 | App list search results update within **≤ 16 ms** per keystroke for 300 apps. |
| NFR-P4 | Idle memory footprint ≤ 60 MB; launcher should avoid being killed by the system. |
| NFR-P5 | No measurable battery drain when idle (no polling loops; use system callbacks). |
| NFR-P6 | APK size ≤ 10 MB. |

### 4.2 Privacy & Security
| ID | Requirement |
|---|---|
| NFR-S1 | No analytics, ads, or third-party trackers in the free tier; any crash reporting is opt-in. |
| NFR-S2 | All usage data, notifications, and settings stored locally only. |
| NFR-S3 | `INTERNET` permission used only for weather and billing. |
| NFR-S4 | Every sensitive permission (Notification access, Usage access, Accessibility, Device Admin) must have an in-app explanation screen and work-around if denied. |
| NFR-S5 | Accessibility Service usage must comply with Google Play policy and be declared in the store listing. |
| NFR-S6 | Publish a clear privacy policy. |

### 4.3 Accessibility
| ID | Requirement |
|---|---|
| NFR-A1 | All interactive elements have content descriptions for TalkBack. |
| NFR-A2 | Minimum touch target 48×48 dp. |
| NFR-A3 | Text contrast meets WCAG AA (4.5:1). |
| NFR-A4 | Respect system font scaling and reduced motion settings. |

### 4.4 Reliability & Compatibility
| ID | Requirement |
|---|---|
| NFR-R1 | Crash-free sessions ≥ 99.8%. |
| NFR-R2 | Test on Pixel, Samsung (One UI), Xiaomi (HyperOS), OnePlus (OxygenOS). |
| NFR-R3 | Handle gesture navigation and 3-button navigation. |
| NFR-R4 | Support portrait (required) and tablets/foldables (graceful layout). |
| NFR-R5 | Handle multiple users and work profiles. |

### 4.5 Localization
| ID | Requirement |
|---|---|
| NFR-L1 | All strings externalized; English at launch. |
| NFR-L2 | Support RTL layouts. |
| NFR-L3 | Date/time formats follow device locale and 12/24-hour setting. |

---

## 5. Technical Architecture (proposed)

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3, heavily restyled to minimal) |
| Architecture | MVVM + unidirectional data flow, single-activity |
| DI | Hilt |
| Persistence | Room (limits, inbox, usage cache), DataStore (preferences) |
| Async | Kotlin Coroutines + Flow |
| Background | WorkManager (digests, daily reset, scheduled focus) |
| Billing | Google Play Billing Library |
| Testing | JUnit, Turbine, Compose UI tests, Macrobenchmark for startup |

### 5.1 Key Android APIs
- `LauncherApps` — enumerate apps incl. work profile, listen for package changes.
- `RoleManager` — request default home role.
- `UsageStatsManager` — screen time & per-app usage.
- `NotificationListenerService` — notification filtering.
- `AccessibilityService` — (opt-in) double-tap lock, enforcing limits outside launcher.
- `NotificationManager` — DND policy access for Focus mode.

### 5.2 Module Structure
```
app/                 – entry point, navigation, DI setup
core/ui/             – theme, typography, shared composables
core/data/           – DataStore, Room, repositories
feature/home/        – F1, F3, F4
feature/applist/     – F2, F5
feature/settings/    – F6, F13
feature/notifications/ – F7
feature/wellbeing/   – F8, F9, F10, F11
feature/widgets/     – F12
feature/onboarding/  – F14
feature/billing/     – F15
feature/shortsblock/ – F16 (Accessibility-based detection + overlay)
```

---

## 6. Screens

1. **Home** — clock, date, battery, favorites list.
2. **App list** — search bar + alphabetical list.
3. **App context menu** — bottom sheet with actions.
4. **Mindful pause / limit-reached** — full-screen interstitial.
5. **Focus session** — timer + allowed apps.
6. **Notification inbox** — grouped list of filtered notifications.
7. **Insights** — daily/weekly usage.
8. **Settings** — Home, App list, Gestures, Appearance, Notifications, Wellbeing, Hidden apps, Backup, Premium, About.
9. **Onboarding** — intro, set default, pick favorites.

---

## 7. User Stories (MVP)

- As a user, I want to see only the time and a few app names when I unlock my phone, so I'm not tempted by colorful icons.
- As a user, I want to type 2–3 letters to find and open any app, so I don't scroll through a grid.
- As a user, I want to swipe right to open the dialer and left to open the camera, so common actions are instant.
- As a user, I want to hide distracting apps, so I only open them when I truly mean to.
- As a user, I want to rename apps, so their labels remind me of my intentions.
- As a user, I want a pure-black theme, so the phone looks calm and saves battery.

---

## 8. Success Metrics

| Metric | Target |
|---|---|
| User-reported daily screen time reduction (survey) | ≥ 20% after 30 days |
| Day-30 retention as default launcher | ≥ 40% |
| Play Store rating | ≥ 4.5 |
| Crash-free sessions | ≥ 99.8% |
| Premium conversion (if F15 enabled) | ≥ 3% of active users |

---

## 9. Milestones

| Milestone | Scope |
|---|---|
| **M0 – Prototype** | Home screen + app list + set as default launcher. |
| **MVP (v1.0)** | F1–F6, F14; Play Store release. |
| **v1.1** | F7 Notification filter, F8 Usage limits, F9 Focus mode. |
| **v1.2** | F10 Insights, F11 Grayscale, F12 Widgets, F13 Backup, F15 Premium. |
| **v1.3** | F16 Short-form content blocker (YouTube Shorts + Instagram Reels first; more apps after measuring rule breakage). |

---

## 10. Risks & Open Questions

| Risk / Question | Mitigation / Notes |
|---|---|
| Google Play scrutiny of Accessibility Service usage | Keep it optional, scoped, and clearly disclosed; core features must work without it. |
| OEMs aggressively killing launcher/background services | Minimize background work; guide users to disable battery optimization if needed. |
| Users can bypass limits by switching launchers | Accept as a non-goal — the app adds friction, not enforcement. |
| Should limits be enforced only via launcher or system-wide? | Default: launcher-only; system-wide via opt-in Accessibility. |
| Free vs. premium split | Validate with early users before v1.2. |
| F16: Target apps change view IDs, silently breaking detection | Data-driven rules (F16.12), remote rule updates (F16.13), automated UI checks against latest app versions. |
| F16: Play Store rejection for non-accessibility use of Accessibility API | Complete the Accessibility declaration form, prominent disclosure + consent, `isAccessibilityTool=false`, demo video for reviewers; keep feature removable via build flag. |
| F16: Android 13+ "restricted settings" for sideloaded installs; Advanced Protection mode blocks non-accessibility tools | Document the steps in-app; degrade gracefully to whole-app limits (F8). |
| F16: Short-form content in browsers (e.g., youtube.com/shorts) not covered | Out of scope for v1.3; consider URL-bar detection later. |
