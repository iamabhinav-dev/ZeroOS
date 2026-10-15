# Changelog

Entries go under **Unreleased** as work lands; they move under a version number when an APK is installed on the phone (Install Checklist).

## Unreleased

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
