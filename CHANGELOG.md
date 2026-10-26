# Changelog

Entries go under **Unreleased** as work lands; they move under a version number when an APK is installed on the phone (Install Checklist).

## Unreleased

## 1.0.0 (versionCode 31), first Stable release (built from branch release-1.0, D-061)
The features of 0.0.27, plus:
- **Before you start:** on first start, and once after an update that changes them, ZeroOS asks you to read the
  README notes, licence and third-party notices of the installed version and tick that you agree (F14.8). They're
  inside the app and also in Settings → About
- **Import warning:** importing a settings backup says that it replaces everything and can't be undone, that
  permissions aren't in a backup, and whether the backup is from an older or newer version (F13.6, F13.7)
- **Safer settings:** imported values are kept within their allowed ranges, and if the settings file ever can't
  be read, a copy is kept before it's reset (D-059)
- **Fixed:** the take-a-break screen appeared at once when you came back to the app within a minute, from recent
  apps or after locking and unlocking the phone
- Not included: the short-video blocker (in the 0.0.28–0.0.30 early builds)
## 0.0.30 (versionCode 30)
- **Before you start:** on first start, and once after an update that changes them, ZeroOS asks you to read the
  README notes, licence and third-party notices of the installed version and tick that you agree (F14.8). They're
  inside the app and also in Settings → About
- **Import warning:** importing a settings backup now says that it replaces everything and can't be undone,
  that permissions aren't in a backup, and whether the backup is from an older or newer version (F13.6, F13.7)
- **Safer settings:** imported values are kept within their allowed ranges, and if the settings file ever can't
  be read, a copy is kept before it's reset (D-059)
## 0.0.29 (versionCode 29)
- Fixed: the take-a-break screen appeared at once when you went home and came back to the app from recent apps
  within a minute
- Only one blocking screen at a time: the limit or break screen comes first; a short-video block waits and shows
  after it if you're still in Shorts or Reels
## 0.0.28 (versionCode 28)
- Short-video blocker (F16), off until you turn it on in Settings → Wellbeing → Short videos: blocks YouTube
  Shorts and Instagram Reels with a black screen (dot shutter over a play symbol, a pun or your own message,
  Go home). Block at once, or after a daily allowance (15 min by default); "Allow 5 minutes" once a day after a
  mindful pause; always blocked during Focus. The rest of each app stays usable
- Insights shows today's short-video time and how often it was blocked
- The Accessibility service can now read screen layout, used only inside YouTube and Instagram while the blocker
  is on; the explanation screens say so. Android may ask you to turn the service on again after updating
## 0.0.27 (versionCode 27)
- Screen time on home (F10): today's screen time and unlocks under the date, in one of four styles: text,
  progress bar against a daily goal, weekly dots or hourly dots. Until Usage access is granted, home shows
  "Tap to show screen time" (× hides it). Tap the stats for Insights: today, top 5 apps, last 7 days
- Settings → Screen time: show on home, style, daily goal, compare with yesterday, Insights
- Focus picker: ‹ and › on Custom are dimmed at 5 and 240 min
## 0.0.26 (versionCode 26)
- Focus picker: a thin line under the search, Start focus as an outlined button, and tapping anywhere on the
  Custom row picks it
- Focus icon twice as big on home and on the Focus screens
- Includes everything from 0.0.24 and 0.0.25 (those releases were replaced by this one)
## 0.0.25 (versionCode 25)
- Back arrow (dotted ‹) at the top of every Settings page, Diagnostics, the Focus picker and the permission
  screens; it goes back to the page you came from, like the Back gesture (D-052)
- Includes everything from 0.0.24 (that release was replaced by this one)
## 0.0.24 (versionCode 24)
- Home shows just the small Focus icon; tap it to start focus. While focusing: the icon, "23m left" and a dot
  line that fills as the time passes
- To end focus early, tap the focus line and type why; "End focus" appears once you have (the reason isn't saved)
- Focus picker: search the apps you can use; custom length with ‹ › to go shorter or longer (5–240 min)
- Back from a picker opened in Settings → Wellbeing → Focus returns to the Focus settings
- Fix: holding the focus line no longer opens Settings
## 0.0.23 (versionCode 23)
- Focus mode (F9): tap "focus" on home, pick 25 / 50 / 90 min or a custom length and the apps you can use, then
  Start. A short "Focus started" screen appears; while focusing, home shows the Focus icon and "focus · 23m
  left", and only your allowed apps appear on home and in the app list. Hold it for 3 s to end early
- Settings → Wellbeing → Focus (shown even with Wellbeing off): show "focus" on home, Focus icon (Mix, Sphere,
  Rim or Flow), Start focus, and turn on Do Not Disturb during focus (put back afterwards)
- Every screen now works in landscape too (two-pane stop, break and Focus screens; wider wallpaper gallery)
- Focus schedules come next (not in this version)
## 0.0.22 (versionCode 22)
- If ZeroOS isn't the default home app when it opens (e.g. a battery-saving mode switched it back), home shows
  "ZeroOS isn't your home app" with **Set as default** (F14.7)
- The background-activity tip in onboarding and the accessibility note are worded for any phone
## 0.0.21 (versionCode 21)
- Puns are always shown on the pause, limit and break screens; the "Puns on stop screens" switch is gone (D-045)
## 0.0.20 (versionCode 20)
- Wellbeing → "Also outside ZeroOS" (needs Accessibility): the limit screen also appears over a limited app
  opened from a notification or recents, and when its limit runs out while you're in it (F8.7)
- Wellbeing → "Take a break" (needs Usage access and Accessibility, D-044): after 15 min straight in a chosen
  app, a black screen with a pun and a button; tap it and the screen goes away after 5 s. Choose apps in
  Wellbeing → Apps; "Break after" 5–60 min, "Wait after tapping" 3–30 s (F8.16–F8.18)
## 0.0.19 (versionCode 19)
- Mindful pause: with "Intention prompt" on, "Open …" appears only once you've typed why you're opening the
  app (D-043)
## 0.0.18 (versionCode 18)
- Wellbeing (Settings → Wellbeing, off until you turn it on, F8): pick apps for a mindful pause and/or a daily
  limit. Opening a paused app from ZeroOS shows breathing dots, a pun and a countdown before "Open …"; an
  app whose limit is used up shows a draining battery with "Go home" and "5 more minutes" (2 a day by
  default). Limits reset at midnight. The app list shows "12m left" / "used up" for limited apps. Turning
  Wellbeing off while a limit is set asks you to hold for 3 s. Needs Usage access for limits
## 0.0.17 (versionCode 17)
- Diagnostics: new Memory tab with ZeroOS's memory samples; a sample is taken 10 s after you leave ZeroOS (the
  previous trigger never fired on the test phone); a failed reading is logged as "memory check failed" (NFR-P4)
## 0.0.16 (versionCode 16)
- Accessibility service (off until you turn it on): double-tap empty space on home to lock the phone
  (Settings → Gestures, F4.5); while it's on, swipe down opens the notification shade instead of search (F4.4).
  It reads nothing on the screen. Gestures warns if it's off, or if the phone stopped it (Step 4.2)
- Settings → Diagnostics shows ZeroOS's memory use; the log records it each time you leave ZeroOS (NFR-P4)
- Note: this APK declares an Accessibility service, so Play Protect may warn when installing (D-018)
## 0.0.15 (versionCode 15)
- Larger back arrow on the full-screen wallpaper preview (F6.8)
- Groundwork for Phase 4: permission explanation screen and checks (Usage access, Accessibility, Do Not
  Disturb); ZeroOS now appears in Android's Usage access list (Step 4.1). Nothing asks for it yet
- First release build for daily use (smaller and faster than the debug builds so far)
## 0.0.14 (versionCode 14)
- Wallpaper preview: swipe left or right to the next or previous wallpaper; it stops at the first and last (F6.8)
## 0.0.13 (versionCode 13)
- Phone wallpapers from ZeroOS (F6.8): Settings → Appearance → Phone wallpaper. A gallery of 17 dot-matrix
  wallpapers plus Pure black / Pure white, showing only those of the current theme. Tap one to see it full
  screen, then Apply → Home screen, Lock screen or Both; Restore previous per screen (D-038)
- Wallpapers are stored as circles and drawn at the phone's exact resolution, so they stay sharp
- Appearance: "Wallpaper" is now called "Home background"; setting a home wallpaper switches it to show the
  phone wallpaper
## 0.0.12 (versionCode 12)
- New logo: the dotted "ZeroOS" wordmark as the app icon, and on the first onboarding page and About (D-037)
## 0.0.11 (versionCode 11)
- Faster cold start: favorites show at once (from what home showed last time) instead of after the app list
  loads; the log now records how long loading the app list takes (NFR-P2)
## 0.0.10 (versionCode 10)
- Fix: with seconds on, the Appearance preview clock wrapped and its digits overlapped; the clock now always
  stays on one line and shrinks to fit (F6.7)
- Home favorites scroll when they don't fit (many favorites, large text, landscape); otherwise nothing changes (F1.6)
## 0.0.9 (versionCode 9)
- Landscape: home shows clock, date and battery on the left and favorites on the right, no more overlap (NFR-R4)
- Optional seconds on the home clock (hh:mm:ss): Settings → Appearance → Show seconds, off by default (F6.7)
## 0.0.8 (versionCode 8)
- Fix: "Uninstall" in the app menu did nothing (Android 9+ needs the REQUEST_DELETE_PACKAGES permission);
  it now opens Android's uninstall dialog, for the right copy of a cloned app (F2.8)
- System apps (e.g. Contacts) no longer offer "Uninstall" (F2.8)
## 0.0.7 (versionCode 7)
- Apps with the same name are numbered everywhere, e.g. "I-WhatsApp" and "II-WhatsApp" for an App Clone
  (D-034). Replaces the "· clone" tag, which home didn't show
- Alphabet scroller: the current letter is larger and brighter; while sliding, the other letters fade (F2.7)
- Swipe down always opens search (notification shade comes with the Accessibility service in v1.1); its
  switch is removed from Settings → Gestures (D-033)
## 0.0.6 (versionCode 6)
- Onboarding on first start: intro, set as default home, pick favorites, battery optimization (Step 2.9).
  Shown once, including after this update
- Settings: long-press home opens Settings (Home, App list, Gestures, Appearance, Backup, Diagnostics,
  About); Diagnostics is now inside Settings (Step 2.11)
- Backup: export and import all settings as a JSON file; Android Auto Backup (Step 2.10)
- Appearance: theme, wallpaper, text size, show clock/date/status bar, with a live preview (Step 2.8)
- Gestures: swipe down opens search (switchable); optional swipe left/right to open an app, off by default (Step 2.7)
- Hide and rename apps from the long-press menu; hidden apps list with optional confirmation (Step 2.6)
- Favorites editor: add, remove, drag to reorder (Step 2.5)
- Rounder app icon: 14 dots along an oval (D-032)
## 0.0.5 (versionCode 5)
- Dot-matrix lightning bolt after the battery % while charging (F1.3, D-031)
- New app icon: Doto's "0" as 12 dots (D-032)
- Fix: no disk reads on the main thread when home opens; StrictMode log no longer flooded by the phone's theme loader

## 0.0.4 (versionCode 4)
- Fix: tapping the clock crashed ZeroOS (the Clock app requires the SET_ALARM permission); clock and date taps
  can no longer crash, failures are logged (Step 2.3)

## 0.0.3 (versionCode 3)
- Doto dot-matrix font for all text; new design system (Step 2.2)
- Settings storage (Step 2.1)
- Home: tap clock for alarms, tap date for calendar, battery %, favorites area (Step 2.3)
- App list: search (initials like "gm"), web/Play Store search, clone tags, swipe down to close (Step 2.4)

## 0.0.2 (versionCode 2)
- Fix: opened as a normal app (not the default launcher), Back now leaves ZeroOS and it no longer blocks
  the system swipe-up-and-hold gesture; it's no longer excluded from Recents (Step 1.1)

## 0.0.1 (versionCode 1), first install on the phone
- Phase 1: home with clock and date, swipe-up app list, Diagnostics (long-press), crash banner, safe mode
- Project setup: Gradle 9.8.1, AGP 9.4.1, Kotlin 2.4.21, Compose, Hilt; core and feature modules (Steps 0.2, 0.3)
- Quality gates: Android Lint, detekt with ZeroOS rules, ktlint, Kover coverage ≥ 80%, Robolectric and Roborazzi tests (Step 0.6)
- Diagnostics core: rotating on-device log, crash reports, previous exit reasons; StrictMode and LeakCanary in debug builds (F17, Step 0.6)
- Placeholder black screen with the app name and a monochrome icon
