# Changelog

Entries go under **Unreleased** as work lands; they move under a version number when an APK is installed on the phone (Install Checklist).

## Unreleased

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
