# ZeroOS Launcher — Requirements & Features

> A personal, minimalist Android home-screen launcher that replaces colorful icons and endless feeds with a calm, text-first interface, so I use my phone intentionally instead of compulsively.

| | |
|---|---|
| **Audience** | Personal use only (one user). Not published to any app store. |
| **Primary device** | One Android phone for manual checks. Must work on any other Android phone (no device-specific code paths). |
| **Platform** | Android (min SDK 26 / Android 8.0, target latest stable SDK) |
| **Distribution** | Signed release APK, sideloaded (copied to the phone and installed via "Install unknown apps"). **No ADB and no Developer options required.** |
| **Type** | Home-screen launcher (`android.intent.category.HOME`) |
| **Status** | Draft v0.2 (personal edition) |
| **Last updated** | 2026-10-15 |

---

## 1. Vision & Goals

### 1.1 Problem
Default launchers are designed for engagement: bright icons, notification badges, and widgets pull me into apps I didn't intend to open. I want to reduce screen time without giving up a smartphone.

### 1.2 Goals
1. **Reduce mindless phone use**: make opening an app a deliberate act.
2. **Remove visual noise**: no icons, no badges, no colors by default.
3. **Stay fast and functional**: every essential app is ≤ 2 interactions away.
4. **Privacy**: no tracking, no ads, all data stays on the device.

### 1.3 Non-Goals
- Not a full OS or ROM replacement.
- Not a parental-control or MDM tool.
- No icon packs, themes, or heavy widget ecosystem.
- **No app store release, monetization, analytics, or multi-user support work.**
- **No features that require ADB, root, or Developer options.**
- No translations or RTL layout work (English only).

---

## 2. Feature Overview

| ID | Feature | Priority | Release |
|---|---|---|---|
| F1 | Minimal home screen (clock, date, favorites) | P0 | v1.0 |
| F2 | Text-based app list with search | P0 | v1.0 |
| F3 | Favorite apps management | P0 | v1.0 |
| F4 | Gestures & shortcuts | P0 | v1.0 |
| F5 | Hide & rename apps | P0 | v1.0 |
| F6 | Appearance customization | P1 | v1.0 |
| F14 | Onboarding | P0 | v1.0 |
| F13 | Backup & restore settings | P0 | v1.0 |
| F7 | Notification filter / inbox | P1 | Deferred (D-039) |
| F8 | App usage limits & mindful pause (toggleable) | P1 | v1.1 |
| F9 | Focus mode | P1 | v1.1 |
| F10 | Screen-time insights & home-screen stats | P1 | v1.1 |
| F11 | Grayscale shortcut | P2 | v1.2 |
| F12 | Minimal widgets (calendar, to-do) | P2 | v1.2 |
| F16 | Short-form content blocker (Reels, Shorts) | P2 | v1.3 |
| F17 | Diagnostics: event log & crash log | P0 | M0 (from the first build) |
| F18 | Safe mode (crash-loop protection) | P0 | M0 (from the first build) |
| ~~F15~~ | ~~Premium / monetization~~ | Removed | Not needed for personal use |

Priority: **P0** = must have, **P1** = should have, **P2** = nice to have.
All features are always available (no paywall).

---

## 3. Functional Requirements

### F1 — Minimal Home Screen
The default screen shown when I press Home.

| ID | Requirement |
|---|---|
| F1.1 | Display current **time** in large text; tapping it opens the default Clock/Alarm app. |
| F1.2 | Display **date** (e.g., "Thursday, 15 October"); tapping it opens the default Calendar app. |
| F1.3 | Optionally display **battery percentage** as plain text. While charging (or full and plugged in), a small dot-matrix lightning bolt in Doto style appears after the percentage (D-031). |
| F1.4 | Display a vertical list of **0–10 favorite apps** as text labels (no icons). |
| F1.5 | No notification badges, dots, or counters on the home screen. |
| F1.6 | Home screen must not scroll; all content fits within one viewport. The layout is designed from v1.0 for **all** home elements that come later (screen-time stats F10, focus button/timer F9, calendar/to-do F12), so adding them never pushes favorites off-screen at the largest font size. |
| F1.7 | Support configurable alignment of the favorites list: left, center, right. |
| F1.8 | Optional: show the next upcoming alarm in small text. |
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
| F2.8 | Long-press an app → context menu: *Add to favorites, Rename, Hide, Set limit (from v1.1), App info, Uninstall*. |
| F2.9 | List updates live when apps are installed/uninstalled/updated. |
| F2.10 | Support apps in other profiles (work profile, or app clones from phone makers' clone features **where Android exposes them via `LauncherApps`**), visually tagged (e.g., "WhatsApp · clone"). |
| F2.11 | Back gesture or swipe down returns to home. |

### F3 — Favorite Apps

| ID | Requirement |
|---|---|
| F3.1 | Add/remove favorites from the app list or settings. |
| F3.2 | Reorder favorites via drag-and-drop in an edit mode. |
| F3.3 | Max favorites configurable (default 6, max 10). |
| F3.4 | If a favorite app is uninstalled, it is removed automatically. |

### F4 — Gestures & Shortcuts

| ID | Requirement |
|---|---|
| F4.1 | **Swipe up** → app list (default). |
| F4.2 | **Swipe left** → assigned app. **Optional, off by default** (side swipes clash with the system back gesture). |
| F4.3 | **Swipe right** → assigned app. **Optional, off by default.** |
| F4.4 | **Swipe down** → expand the notification shade while the optional Accessibility Service is enabled (`GLOBAL_ACTION_NOTIFICATIONS`); otherwise (and until that service exists) it opens search. Not a setting (D-033). |
| F4.5 | **Double-tap** → lock screen (uses the optional Accessibility Service; off by default). |
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
| F6.3 | Font: **Doto** (OFL dot-matrix, look-alike of Nothing's Ndot 57) for **all text in the launcher**: clock, date, numbers, app names, lists, settings, dialogs, onboarding and stop screens. No font choice. |
| F6.4 | Text size: small / medium / large / extra-large; respects system font scale. Sizes are kept large enough for Doto to stay readable (minimum 14 sp for body text). |
| F6.5 | Toggle visibility of clock, date, battery, status bar. |
| ~~F6.6~~ | ~~Optional single accent color~~ (removed: ZeroOS stays black and white, D-035). |
| F6.7 | Clock shows **hh:mm** or **hh:mm:ss** (show seconds on/off); it shrinks to fit one line if needed (D-036). |
| F6.8 | **Phone wallpaper from ZeroOS** (Settings → Appearance): pick from a small built-in gallery (thumbnail + name); tapping one opens it full screen (swipe left/right to the neighbouring ones, without wrapping around) with a back arrow and **Apply**, which asks **Home screen**, **Lock screen** or **Both**; a screen not chosen is left unchanged. Uses only Android's public `WallpaperManager` (`FLAG_SYSTEM` / `FLAG_LOCK`). Gallery: generated **Pure black** and **Pure white**, plus monochrome images added later from `docs/brand/wallpapers/` (no code change per image). The gallery shows only wallpapers matching the current theme: dark ones (Pure black, dark images) in dark mode, light ones (Pure white, light images) in light mode; with theme "System" it follows the phone's dark mode. Images are centre-cropped to the phone's real screen size, never stretched, decoded off the main thread and downsampled. "Restore previous" (per screen) brings back the wallpaper ZeroOS set there before; the phone's own earlier wallpaper can't be read by apps, which the screen says. If the phone rejects a separate lock-screen wallpaper, the screen says so (D-038). |

### F7 — Notification Filter (deferred, D-039)
Kept for later; not built in v1.1.

| ID | Requirement |
|---|---|
| F7.1 | Uses `NotificationListenerService` (opt-in, with a short explanation). |
| F7.2 | I select **allowed apps** whose notifications come through normally. |
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
| F8.7 | Enforcement outside the launcher (e.g., opening via notification) is **best-effort** using the optional Accessibility Service. |
| F8.8 | **Master on/off toggle** for the whole feature in Settings → Wellbeing; **off by default**. When off, no pause screens, limits, or remaining-time labels are shown and no usage tracking runs. |
| F8.9 | **Individual toggles** for each part: Mindful pause (F8.1), Intention prompt (F8.2), Daily limits (F8.3–F8.6), and Outside-launcher enforcement (F8.7). Each can be turned on/off independently while the master toggle is on. |
| F8.10 | Turning the feature off **keeps the configuration** (flagged apps, limits, pause duration) so it's restored when turned back on. |
| F8.11 | Permissions (Usage Access, Accessibility) are requested only when the related toggle is first switched on; switching off stops the related background work immediately. |
| F8.12 | Optional friction when turning off during an active limit (e.g., mindful pause or "hold for 3 s"), configurable, to prevent impulsive disabling. |
| F8.13 | **Puns on stop screens:** the mindful pause (F8.1) shows a random line from a "pause" list (e.g. "Is this a want, or a habit?"); the limit-reached block (F8.4, F8.7) shows a random line from a "limit" list (e.g. "That's a wrap for today."). Lists of ~20 lines each (approved text in `docs/puns.md`) become string-arrays in `core/resources`. **Shuffle-bag order:** every line once before any repeat, never the same line twice in a row; the position survives restarts. |
| F8.14 | Puns are **always shown** on the stop screens; there is no setting to turn them off (D-045). |
| F8.15 | **Dot animations** (monochrome, dot-matrix style, above the pun): the mindful pause shows **breathing dots** that slowly grow and shrink in time with the countdown; the limit-reached screen shows a **battery draining** to empty. They run only while the screen is visible, and are static when system "Remove animations" / reduced motion is on (NFR-A4). |
| F8.16 | **Take a break** (D-044): when a chosen app has been in front for 15 min straight, a black screen covers it with a "break" pun (list D in `docs/puns.md`) and one button whose label is a "more time" pun (list E). The screen stays until the button is tapped; then a 5 s countdown runs on the button and the screen disappears, and the timer starts again. Back doesn't close it. Needs the Accessibility service (works outside ZeroOS, like F8.7). |
| F8.17 | Leaving the app or turning the screen off for at least 1 min resets the take-a-break timer; shorter switches don't. Timers, not polling (NFR-P1). |
| F8.18 | Settings → Wellbeing: "Take a break" switch, "after" (5–60 min) and "wait after tapping" (3–30 s). Apps are chosen per app in the Apps list, next to the pause and the limit. |

### F9 — Focus Mode

| ID | Requirement |
|---|---|
| F9.1 | A text button **"focus"** on the home screen starts Focus mode with one tap (opens the duration + allow-list picker). It can be hidden in Settings; Focus can also be started from Settings. While Focus is active, the button is replaced by the remaining time (F9.4), and holding it ends the session (F9.5). |
| F9.2 | Pick a duration (25 / 50 / 90 min / custom) and an allow-list of apps. |
| F9.3 | During focus: only allowed apps are shown/launchable; notification filter is maximal (once F7 is built, D-039; until then Do Not Disturb, F9.7, keeps it quiet). |
| F9.4 | Home screen shows remaining focus time. |
| F9.5 | Ending early requires a deliberate action (e.g., hold for 3 s). |
| F9.6 | Optional scheduled focus sessions (e.g., weekdays 09:00–12:00). |
| F9.7 | Optionally toggle system Do Not Disturb (requires DND access). |
| F9.8 | **Focus icon** (D-047): a dotted circle with the F as the empty gap, chosen from four designs: Mix, Sphere, Rim, Flow (`docs/brand/focus-marks/`). Drawn in the theme colour. |
| F9.9 | The Focus icon shows (a) big at the top of the Focus picker; (b) on a **"Focus started"** screen after "Start focus" (tap to skip); (c) small next to "focus · 23m left" on home while focus runs; (d) as a preview on the "Focus icon" settings row. |
| F9.10 | Focus settings live in **Settings → Wellbeing → Focus**. The Focus row is shown even when Wellbeing is off, and Focus works independently of the Wellbeing switch (which only controls F8). |

### F10 — Screen-Time Insights

| ID | Requirement |
|---|---|
| F10.1 | Daily total screen time and number of unlocks. |
| F10.2 | Top 5 apps by usage today, plus 7-day trend (simple text/bar, monochrome). |
| F10.3 | **Home-screen stats**: show today's **total screen time** and **number of unlocks** on the home screen. On by default; can be turned off. |
| F10.4 | All data computed locally from `UsageStatsManager`; nothing uploaded. |
| F10.5 | Home-screen stats **display style** (one selectable): **Text** (e.g., "2h 14m · 38 unlocks"), **Progress bar** (screen time vs. a daily goal, plus unlock count), **Weekly bars** (7 small bars for screen time, today highlighted, plus unlock count), or **Hourly timeline** (24 thin bars of today's screen time per hour, plus unlock count). |
| F10.6 | All styles are monochrome, small, and fit on the non-scrolling home screen (F1.6) without pushing favorites off-screen. Charts are drawn with Compose `Canvas` (no chart library). |
| F10.7 | Optional **daily screen-time goal** (used by the progress bar); when exceeded, the bar fills completely and the text says "over by Xm". No colors or alerts. |
| F10.8 | Optional comparison with yesterday (e.g., "↓ 12% vs yesterday"). |
| F10.9 | Values refresh when the home screen becomes visible and when the screen turns on. No background polling. |
| F10.10 | Tapping the stats opens the Insights screen (F10.1–F10.2). |
| F10.11 | Needs Usage Access. Until granted, the home shows one subtle line "Tap to show screen time" that opens the permission explanation; if declined, the line can be dismissed and the stats stay hidden. |
| F10.12 | Unlock count uses `KEYGUARD_HIDDEN` usage events (Android 9+). On Android 8.x, unlocks are hidden and only screen time is shown. |
| F10.13 | Each chart has a content description stating its values (e.g., "Screen time 2 hours 14 minutes, 38 unlocks") for accessibility. |

### F11 — Grayscale Shortcut
Android stores grayscale (color correction) in *Secure Settings*, which third-party apps can only change with `WRITE_SECURE_SETTINGS`, a permission grantable only via ADB. Since ADB is out of scope, ZeroOS helps me use the **system's own** grayscale instead.

| ID | Requirement |
|---|---|
| F11.1 | Settings → Grayscale opens the system color-correction screen (`Settings.ACTION_ACCESSIBILITY_SETTINGS` or the closest available screen), with one line of instructions to choose **Grayscale**. |
| F11.2 | Show a tip to assign color correction to the system **accessibility shortcut** (e.g., hold both volume keys) for instant on/off. |
| F11.3 | No ADB-based toggling, scheduling, or `WRITE_SECURE_SETTINGS` usage. |

### F12 — Minimal Widgets

| ID | Requirement |
|---|---|
| ~~F12.1~~ | ~~Weather~~ (removed: not needed; keeps the app fully offline). |
| F12.2 | **Calendar**: next event text line. |
| F12.3 | **To-do**: up to 3 short tasks, editable inline, stored locally. |
| F12.4 | All widgets are text-only and individually toggleable; off by default. |

### F13 — Backup & Restore

| ID | Requirement |
|---|---|
| F13.1 | Export all settings (favorites, hidden, renames, limits, gestures, F16 settings) to a JSON file. |
| F13.2 | Import from file with confirmation. |
| F13.3 | Support Android Auto Backup for settings. |
| F13.4 | Available from v1.0. Every later feature that adds settings must add them to the export/import format in the same step, with a round-trip test. |
| F13.5 | The backup file has a schema version; importing a file from an older version works (missing fields get Appendix A defaults). |

### F14 — Onboarding

| ID | Requirement |
|---|---|
| F14.1 | 3–4 screen intro explaining the philosophy. |
| F14.2 | Prompt to set ZeroOS as default launcher (`RoleManager.ROLE_HOME` on Android 10+, otherwise open home settings). |
| F14.3 | Pick initial favorites (suggest Phone, Messages, Camera, Maps). |
| F14.4 | Permissions requested **just-in-time** when the related feature is enabled, never all upfront. |
| F14.5 | Onboarding is skippable. |
| F14.6 | **Background reliability step**: detect if battery optimization is on for ZeroOS and offer to open the relevant screen (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`). Show plain-text tips for phones that restrict background apps as plain text; never depend on OEM-private APIs. |
| F14.7 | **Not the home app** (D-049): whenever ZeroOS comes to the front and isn't the default home app (some phones' battery-saving modes switch it back to the stock launcher), home shows one line at the top: "ZeroOS isn't your home app · Set as default". Tapping it opens Android's "default home app" dialog (`RoleManager.ROLE_HOME`; home settings before Android 10). The line disappears once ZeroOS is the default again; "Dismiss" hides it until ZeroOS next comes to the front. Each detection is logged (F17.7). |

### ~~F15 — Monetization~~ (removed)
Not needed for personal use. All features are always available.

### F16 — Short-Form Content Blocker
Blocks or time-limits short-form video feeds (YouTube Shorts, Instagram Reels) **inside** other apps, showing a minimal black screen with a custom message. The rest of each app stays usable.

**Approach:** Extends the optional Accessibility Service from F8.7. No official Android API exists for this, so detection relies on matching screen elements (view resource IDs) of target apps. When those apps change their UI, I rebuild ZeroOS with updated rules.

| ID | Requirement |
|---|---|
| F16.1 | Feature is **opt-in and off by default**; the launcher is fully functional if Accessibility permission is denied. |
| F16.2 | Before enabling, show a short explanation screen of what the service reads (only the on-screen structure of selected apps) and how to allow it on Android 13+ (App info → ⋮ → **Allow restricted settings**). |
| F16.3 | Supported apps at launch: **YouTube Shorts** and **Instagram Reels**. Others (TikTok, Facebook Reels, Snapchat Spotlight) only if I ask later. |
| F16.4 | Enable/disable blocking per supported app. |
| F16.5 | The service reads **screen content only from the selected target apps**. It also receives window-change events from all apps, but uses only the app name, to know which app is in front (F16.18). |
| F16.6 | Detection uses targeted lookups (`findAccessibilityNodeInfosByViewId`) on window-state/content-change events, **throttled to ≤ 1 check per 300–500 ms**; no full-tree traversal. |
| F16.7 | **Block mode:** on detection, immediately show a full-screen black overlay (`TYPE_ACCESSIBILITY_OVERLAY`) with the block message (F16.9) and a **"Go home"** button. |
| F16.8 | **Limit mode:** track time spent in short-form content per day; show the block overlay once the daily allowance (e.g., 15 min) is used up. Allowance resets at local midnight. |
| F16.9 | **Block message:** by default a **random pun** from a built-in list of ~20 lines (`docs/puns.md`, list A → string-array in `core/resources`), in the same shuffle-bag order as F8.13. Option: use my own custom message instead (editable, max 120 chars). |
| F16.10 | **"Go home"** performs `GLOBAL_ACTION_HOME` (leaves the app for the ZeroOS home screen; Android doesn't let apps force-close other apps) and dismisses the overlay. Optionally go home automatically after N seconds. **No grace period:** the overlay shows again as soon as short-form content is detected; the only pause is ~1 s while going home, to avoid flicker. A single Back press is not used because it often stays on the Shorts tab. |
| F16.11 | Optional friction to bypass: "Allow 5 minutes" after a mindful pause (reuses the F8.1 screen; works even if F8 is toggled off), limited to N times/day. |
| F16.12 | Detection rules (package → view IDs) are stored as **versioned data in a bundled asset file**, not hard-coded in logic. Updating rules = edit the file + rebuild. |
| ~~F16.13~~ | ~~Remote rule updates~~ (removed: no network, no rule server). |
| F16.14 | Show today's short-form time and block count in Insights (F10). |
| F16.15 | Detect and warn when the service has been killed or disabled by the system/OEM, with guidance to allow background activity. |
| F16.16 | Respect Focus mode (F9): during focus, short-form content is always blocked regardless of limits. |
| F16.17 | **Dot animation** on the block screen: a dot-matrix **shutter closing** over a play symbol, once, when the screen appears; then still. Static when reduced motion is on (NFR-A4). |
| F16.18 | The overlay shows only while YouTube/Instagram is the app actually in front (not its preview in Recents), and stays up through short gaps (< 1 s) while a Short loads or changes, so it doesn't blink. Requires listening to window changes from all apps (app name only, no content); screen content is read only from target apps (updates F16.5). |

**Performance budget (additional, on top of NFR-P):**
| Metric | Target |
|---|---|
| Extra battery drain | ≤ 2% per day with heavy target-app use |
| Extra memory | ≤ 20 MB |
| Overlay appearance latency after detection | ≤ 300 ms |
| APK size increase | ≤ 100 KB |

### F17 — Diagnostics: Event Log & Crash Log
A local, on-device log of what the app is doing, so that when something breaks or crashes I can see what happened. Built **first** (Phase 0–1) and used in every version. Logs never leave the phone unless I export them.

| ID | Requirement |
|---|---|
| F17.1 | All logging goes through one `Logger` API with levels **DEBUG, INFO, WARN, ERROR**. No direct `android.util.Log` or `println` calls in app code. |
| F17.2 | Log entries are written to **rotating files in the app's private storage** (`filesDir/logs/`): each line has timestamp, level, tag/module, message, and stack trace for errors. |
| F17.3 | Size and age limits: max **3 files × 1 MB**, entries older than **7 days** deleted. Logging can never fill storage. |
| F17.4 | Writes happen on a background thread with buffering; logging never blocks the main thread or slows the UI (NFR-P1). |
| F17.5 | **Crash log:** an uncaught-exception handler synchronously writes the crash (thread, exception, full stack trace, app version, last 50 log entries) to a separate `crash-<timestamp>.txt` file, then passes the crash to the system's default handler. |
| F17.6 | **Previous exit reason:** on every start, read `ActivityManager.getHistoricalProcessExitReasons` (Android 11+) and log why the app or its services last stopped: crash, ANR, low memory, killed by the system/OEM, or user. Especially useful to see the phone killing background services. |
| F17.7 | **Events logged** (at minimum): app start (version, build type, Android version, device model); screen opened; app launched from launcher (package name); setting changed (key + new value); permission granted/denied/revoked; services connected/disconnected/killed (Notification listener, Accessibility); WorkManager job start/finish/fail; F8 pause/limit/block events; F9 focus start/end; F16 detection and block events (package + rule ID); every caught exception. |
| F17.8 | **Never logged:** notification titles/text, intention text (F8.2), typed search queries, to-do text, custom app names (F5.4), the custom block message (F16.9), or any content read from other apps' screens. Free-text setting values are logged as `<redacted, N chars>`. Package names and original app labels are allowed. |
| F17.9 | **Diagnostics screen** (Settings → Diagnostics): view recent logs (newest first, filter by level), view crash reports, **Export/Share** (zip of all log files via the Android share sheet or save-to-file), and **Clear logs**. |
| F17.10 | After a crash, the next app start shows a small one-time message "ZeroOS crashed last time. View report?" that opens the crash report. |
| F17.11 | Settings toggle **Diagnostics logging** (on by default) and **log level**: DEBUG in debug builds, INFO in release builds by default. Crash logging (F17.5) and exit reasons (F17.6) are always on. |
| F17.12 | Logging must not crash the app: any failure inside the logger is swallowed. |

### F18 — Safe Mode (Crash-Loop Protection)
As the default home app, a crash on start would repeat on every Home press and make the phone hard to use. Safe mode keeps the phone usable.

| ID | Requirement |
|---|---|
| F18.1 | Record each app start and each crash (time stamps in a tiny file written synchronously, separate from DataStore). |
| F18.2 | If the app crashes **3 times within 60 seconds of starting**, the next start enters **Safe mode**. |
| F18.3 | Safe mode shows only: a plain alphabetical app list (tap to launch, no search ranking, no gestures), a banner "Safe mode: ZeroOS crashed repeatedly", and buttons **View crash report** (F17), **Open Home settings** (`Settings.ACTION_HOME_SETTINGS`, to switch to the stock launcher), and **Exit safe mode**. |
| F18.4 | Safe mode does not read DataStore/Room, start services, or apply any feature (limits, focus, F16, stats), so corrupted settings or a buggy feature cannot crash it. |
| F18.5 | Safe mode depends only on `LauncherApps`, `core/logging` and `core/resources`; it lives in `app/` and is kept minimal. |
| F18.6 | "Exit safe mode" resets the crash counter and restarts normally. A start without a crash for 60 s also resets the counter. |
| F18.7 | **Never disable or uninstall the phone's stock launcher**: it is the fallback if ZeroOS is unusable. Documented in README and onboarding. |

---

## 4. Non-Functional Requirements

### 4.1 Performance
| ID | Requirement |
|---|---|
| NFR-P1 | Home screen appears with no visible delay after pressing Home (target ≤ 100 ms warm). |
| NFR-P2 | Cold start ≤ 500 ms on the primary device. |
| NFR-P3 | App list search results update within **≤ 16 ms** per keystroke for 300 apps (verified by a JVM unit benchmark of the matching function). |
| NFR-P4 | Idle memory footprint ≤ 80 MB (measured in the background; was 60 MB, D-042). |
| NFR-P5 | No noticeable battery drain when idle (no polling loops; use system callbacks). |
| NFR-P6 | APK size ≤ 10 MB. |

Until device-based benchmarks are re-enabled, NFR-P1/P2/P4/P5 are verified **manually** on the phone (see the manual check in IMPLEMENTATION_PLAN.md).

### 4.2 Privacy & Security
| ID | Requirement |
|---|---|
| NFR-S1 | No analytics, ads, crash reporting services, or third-party trackers. |
| NFR-S2 | All usage data, notifications, and settings stored locally only. |
| NFR-S3 | The app never requests the `INTERNET` permission; it works fully offline. |
| NFR-S4 | Every sensitive permission (Notification access, Usage access, Accessibility, DND) has a short in-app explanation and a working fallback if denied. |
| NFR-S5 | Logs stay on the device in private storage and are only shared when I export them. They never contain the private content listed in F17.8. |
| NFR-S6 | The release signing keystore is kept outside the repo and backed up **off this computer** (password manager / encrypted cloud); losing it means reinstalling and losing settings. |
| NFR-S7 | Source code is backed up to a **private** GitHub repository; `main` is pushed after every completed step. |

### 4.3 Accessibility
| ID | Requirement |
|---|---|
| NFR-A1 | All interactive elements have content descriptions. |
| NFR-A2 | Minimum touch target 48×48 dp. |
| NFR-A3 | Text contrast meets WCAG AA (4.5:1). |
| NFR-A4 | Respect system font scaling and reduced motion settings. |

### 4.4 Reliability & Compatibility
| ID | Requirement |
|---|---|
| NFR-R1 | No crashes or ANRs in daily use on the primary device. |
| NFR-R2 | Manual checks on one primary phone. Code uses only public Android APIs so it works on other Android phones (Pixel, Samsung, etc.) without changes. |
| NFR-R3 | Handle gesture navigation and 3-button navigation. |
| NFR-R4 | Portrait layout required; landscape must not crash. |
| NFR-R5 | Handle work-profile / app-clone apps. |
| NFR-R6 | Settings and data survive app updates (installing a new APK over the old one). |
| NFR-R7 | Survive process death and configuration changes (rotation, dark-mode switch, font-size change) without losing state. |
| NFR-R8 | A crash loop never locks me out of the phone (F18). |

### 4.5 Language, Text & Formats
| ID | Requirement |
|---|---|
| NFR-L1 | English only. |
| NFR-L2 | Date/time formats follow device locale and 12/24-hour setting. |
| NFR-L3 | **Single source for all text:** every piece of text shown to me (titles, labels, buttons, messages, errors, dialog text, notification text, content descriptions, and default texts such as the F16.9 block message) is defined once in the `core/resources` module (`res/values/strings.xml` and `plurals.xml`). Changing it there changes it everywhere. |
| NFR-L4 | **No hardcoded text** in Kotlin or Compose code: screens use `stringResource(R.string.…)` / `pluralStringResource(…)`. |
| NFR-L5 | **Reuse, don't duplicate:** the same concept uses the same string resource across screens (e.g., one `common_go_back` for every "Go back" button). Naming: `common_*` for shared text, `<screen>_*` for screen-specific text (e.g., `home_tap_to_show_screen_time`). |
| NFR-L6 | Dynamic text uses placeholders (`%1$s`, `%1$d`) and plurals (e.g., "1 unlock" / "38 unlocks"), never string concatenation. |
| NFR-L7 | The build **fails** if hardcoded user-facing text is found (Android Lint `HardcodedText` as error, plus a detekt rule for string literals passed to `Text()`, `contentDescription`, and similar UI parameters). |
| NFR-L8 | Non-text UI values follow the same principle: colors, sizes, spacing, and durations come from theme tokens in `core/ui`, never hardcoded in screens. |

---

## 5. Technical Architecture

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3, heavily restyled to minimal) |
| Architecture | MVVM + unidirectional data flow, single-activity |
| DI | Hilt |
| Persistence | DataStore (all settings, incl. per-app rules and focus schedules, D-041); Room only if a later step needs it (e.g. a usage cache) |
| Async | Kotlin Coroutines + Flow |
| Background | WorkManager (digests, daily reset, scheduled focus) |
| Testing | JVM only for now: JUnit, Turbine, Robolectric, Compose UI tests on Robolectric, Roborazzi screenshots |

### 5.1 Key Android APIs
- `LauncherApps`: enumerate apps incl. work/clone profiles, listen for package changes.
- `RoleManager`: request default home role.
- `UsageStatsManager`: screen time & per-app usage.
- `NotificationListenerService`: notification filtering.
- `AccessibilityService`: (opt-in) double-tap lock, limits outside launcher, short-form blocker.
- `NotificationManager`: DND policy access for Focus mode.
- `PowerManager`: battery-optimization status.

### 5.2 Module Structure
```
app/                   – entry point, navigation, DI setup
core/model/            – plain data models
core/ui/               – theme tokens (colors, typography, spacing), shared composables
core/resources/        – all user-facing text: strings.xml, plurals.xml (NFR-L3)
core/data/             – DataStore, Room, repositories
core/testing/          – fakes and test utilities (test-only)
core/logging/          – F17 Logger, file writer, crash handler, exit reasons
feature/home/          – F1, F3, F4
feature/applist/       – F2, F5
feature/settings/      – F6, F11, F13
feature/onboarding/    – F14
feature/notifications/ – F7
feature/wellbeing/     – F8, F9, F10
feature/widgets/       – F12
feature/shortsblock/   – F16
```

---

## 6. Screens

1. **Home**: clock, date, battery, screen-time & unlock stats, favorites list.
2. **App list**: search bar + alphabetical list.
3. **App context menu**: bottom sheet with actions.
4. **Mindful pause / limit-reached**: full-screen interstitial.
5. **Focus session**: timer + allowed apps.
6. **Notification inbox**: grouped list of filtered notifications.
7. **Insights**: daily/weekly usage.
8. **Settings**: Home, App list, Gestures, Appearance, Notifications, Wellbeing, Short-form blocker, Grayscale, Hidden apps, Backup, About.
9. **Onboarding**: intro, set default, pick favorites, background reliability.
10. **Short-form block overlay**: black screen + pun or custom message + "Go home".
11. **Diagnostics**: log viewer, crash reports, export, clear.
12. **Safe mode**: plain app list, crash banner, crash report, home settings, exit.

Low-fidelity wireframes live in `docs/wireframes.md`. They are approved **phase by phase** (Step 0.4, 2.0, 4.0, 5.0, 6.0) before that phase's screens are built; the home screen is drawn in its final form in Step 0.4.

---

## 7. User Stories

- I want to see only the time and a few app names when I unlock my phone, so I'm not tempted by colorful icons.
- I want to type 2–3 letters to find and open any app, so I don't scroll through a grid.
- I want to swipe right to open the dialer and left to open the camera, so common actions are instant.
- I want to hide distracting apps, so I only open them when I truly mean to.
- I want to rename apps, so their labels remind me of my intentions.
- I want a pure-black theme, so the phone looks calm and saves battery.
- I want to see today's screen time and unlock count on my home screen, so I notice how much I'm using the phone.
- I want Shorts and Reels blocked with a black screen, so I don't get pulled into endless scrolling.

---

## 8. Personal Success Criteria

| Criterion | Target |
|---|---|
| Daily screen time (from F10) | ≥ 20% lower after 30 days vs. the week before switching |
| Still using ZeroOS as default launcher | After 30 days |
| Crashes / ANRs noticed in daily use | 0 per week |

---

## 9. Milestones

| Milestone | Scope |
|---|---|
| **M0 – Prototype** | Home screen + app list + set as default launcher. |
| **v1.0 – Daily driver** | F1–F6, F13, F14; installed on my phone as the default launcher. |
| **v1.1** | F8 Usage limits, F9 Focus mode, F10 Insights & home-screen stats (F7 Notification filter deferred, D-039). |
| **v1.2** | F11 Grayscale shortcut, F12 Widgets (calendar, to-do). |
| **v1.3** | F16 Short-form content blocker (YouTube Shorts + Instagram Reels). |

---

## 10. Risks & Open Questions

| Risk / Question | Mitigation / Notes |
|---|---|
| Some phone makers aggressively kill background services (notification listener, Accessibility Service) | F14.6 background reliability step; F16.15 warning; feasibility spike in Step 0.5. |
| Android 13+ "restricted settings" block Accessibility/Notification access for sideloaded apps | Explain in-app: App info → ⋮ → Allow restricted settings. No Developer options needed. |
| Play Protect (enhanced fraud protection, in some regions) blocks sideloaded APKs that declare notification-listener or Accessibility services | v1.0 is unaffected (declares neither). From v1.1: pause Play Protect scanning while installing, then re-enable (D-018). Fallbacks: Play Console internal testing track; Google's hobbyist developer verification when available. Verified in spike Step 0.5. |
| `UsageStatsManager` accuracy differs per OEM | Spike in Step 0.5; treat usage numbers as approximate. |
| Target apps change view IDs, silently breaking F16 detection | Rules in a data file (F16.12); detection tests use recorded fixtures; rebuild when broken. |
| Short-form content in browsers (e.g., youtube.com/shorts) not covered | Out of scope for v1.3. |
| Lost signing keystore | Back it up outside the repo (NFR-S6). |
| I can bypass limits by switching launchers | Accepted: the app adds friction, not enforcement. |

---

## Appendix A — Default Values

Agents must use these values instead of guessing. Change them here only with my approval.

| Setting | Default | Range / Options |
|---|---|---|
| Max favorites (F3.3) | 6 | 1–10 |
| Default favorites (F14.3) | Phone, Messages, Camera, Maps | any |
| Swipe left / right (F4.2, F4.3) | Off | off / an app |
| Swipe down (F4.4) | Search; notification shade when Accessibility Service is on | not configurable (D-033) |
| Swipe distance threshold | 48 dp | 32–96 dp |
| Swipe velocity threshold | 300 dp/s | — |
| Double-tap to lock (F4.5) | Off | on/off |
| Keyboard auto-open (F2.3) | On | on/off |
| Auto-launch single match (F2.5) | Off | on/off |
| Fast-scroller (F2.7) | Off | on/off |
| Hidden apps confirmation (F5.3) | Off | off / confirm / 5 s delay |
| Theme (F6.1) | Dark (pure black) | dark, light, system |
| Clock seconds (F6.7) | Off (hh:mm) | on/off |
| Phone wallpaper (F6.8) | Pure black selected (Pure white in light mode); nothing applied until I choose | gallery item of the current theme; home / lock / both, chosen on Apply |
| Text size (F6.4) | Medium | S / M / L / XL |
| Favorites alignment (F1.7) | Left | left, center, right |
| Battery % / next alarm (F1.3, F1.8) | On / Off | on/off |
| Notification digest times (F7.4) | Off; 12:00 & 18:00 when on | any times |
| F8 master toggle (F8.8) | Off | on/off |
| Mindful pause length (F8.1) | 5 s | 3–10 s |
| Intention prompt (F8.2) | Off | on/off |
| Daily app limit (F8.3) | 30 min | 5–240 min |
| "5 more minutes" uses/day (F8.4) | 2 | 0–5 |
| Take a break (F8.16) | Off | on/off |
| Take a break after (F8.18) | 15 min | 5–60 min |
| Take a break wait after tapping (F8.18) | 5 s | 3–30 s |
| Take a break reset gap (F8.17) | 1 min | — |
| Disable friction (F8.12) | Hold 3 s | off / hold 3 s / pause |
| Focus button on home (F9.1) | Shown | shown / hidden |
| Focus durations (F9.2) | 25 / 50 / 90 min | custom 5–240 min |
| End focus early (F9.5) | Hold 3 s | — |
| Focus toggles DND (F9.7) | Off | on/off |
| Focus icon (F9.8) | Mix | Mix / Sphere / Rim / Flow |
| "Focus started" screen (F9.9) | 2 s, tap to skip | — |
| F16 feature toggle (F16.1) | Off | on/off |
| F16 mode (F16.7/F16.8) | Block | block / limit |
| F16 daily allowance (F16.8) | 15 min | 0–120 min |
| F16 block message (F16.9) | Random pun | random pun / custom (≤ 120 chars) |
| Puns on pause / limit / break screens (F8.13, F8.14) | Always on (D-045) | — |
| F16 auto-home after (F16.10) | Off | off / 3–10 s |
| F16 pause after "Go home" before blocking again (F16.10) | 1 s | fixed |
| F16 "Allow 5 minutes" uses/day (F16.11) | 1 | 0–5 |
| F16 detection throttle (F16.6) | 400 ms | 300–500 ms |
| Home-screen stats (F10.3) | On (asks for Usage Access first) | on/off |
| Home-screen stats style (F10.5) | Text | text / progress bar / weekly bars / hourly timeline |
| Daily screen-time goal (F10.7) | Off; 2 h when on | 30 min–8 h |
| Compare with yesterday (F10.8) | Off | on/off |
| To-do items (F12.3) | 3 max | — |
| Daily reset time (F8.6, F16.8) | Local midnight | — |
| Diagnostics logging (F17.11) | On | on/off |
| Log level (F17.11) | DEBUG (debug build) / INFO (release build) | DEBUG / INFO / WARN / ERROR |
| Log size limit (F17.3) | 3 files × 1 MB | — |
| Log retention (F17.3) | 7 days | 1–30 days |
| Crash entries attached to crash report (F17.5) | Last 50 log entries | — |
| Safe mode trigger (F18.2) | 3 crashes within 60 s of start | — |
