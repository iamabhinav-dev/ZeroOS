# Decisions Log

Short records of decisions already made. Agents must not reopen these; if one looks wrong, ask the user.
Format: **D-NNN — Title** · date · status · decision · why.

---

**D-001 — Personal use only** · 2026-10-15 · Accepted
The app is for one user. No app store, monetization (F15 removed), analytics, multi-user, or translation work.
*Why:* Removes large amounts of store, policy and compatibility work that a single user doesn't need.

**D-002 — Primary device: the primary phone, but generic Android code** · 2026-10-15 · Accepted
Manual checks happen on the primary phone. Code uses only public Android APIs so it works on other phones.
*Why:* It's the user's phone, and they want to be able to switch phones later.

**D-003 — Sideloaded signed APK, no ADB / Developer options** · 2026-10-15 · Accepted
Install by copying a signed release APK to the phone. Nothing may depend on ADB, root, or Developer options.
*Why:* The user doesn't want Developer options enabled on the phone.
*Consequences:* Android 13+ "restricted settings" must be allowed manually for Accessibility/Notification access; the signing keystore must be backed up.

**D-004 — Grayscale via system setting, not ADB** · 2026-10-15 · Accepted
F11 opens the system color-correction setting and suggests the accessibility shortcut. No `WRITE_SECURE_SETTINGS`.
*Why:* That permission can only be granted via ADB (see D-003).

**D-005 — JVM-only automated tests for now** · 2026-10-15 · Accepted
Unit, Robolectric UI, Roborazzi screenshot, and Robolectric persistence tests. Instrumented/emulator tests and Macrobenchmark are deferred. Device behaviour is verified with manual checks on the phone.
*Why:* Running tests on the phone needs Developer options (D-003); the user chose to skip emulator tests for now.

**D-006 — No remote F16 rule updates** · 2026-10-15 · Accepted
Short-form detection rules ship in a bundled asset file; when target apps change, update the file and rebuild (F16.13 removed).
*Why:* Avoids network access, a rule server, and signing infrastructure for a single user.

**D-007 — Keep onboarding** · 2026-10-15 · Accepted
F14 stays, including the intro pages and a background-reliability step (F14.6) for battery settings.
*Why:* The user's choice.

**D-008 — No weather; app is fully offline** · 2026-10-15 · Accepted
F12.1 weather removed. The app never requests `INTERNET`.
*Why:* Not needed; also removes location permission and a third-party API.

**D-009 — Screen time & unlocks on the home screen, in v1.1** · 2026-10-15 · Accepted
F10 home-screen stats (text, progress bar, weekly bars, or hourly timeline) are on by default. F10 moves from v1.2 to v1.1 because it reuses the usage-stats code built for F8.
*Why:* User wants daily usage visible on the home screen.

**D-010 — Debug and release signed with the same key** · 2026-10-15 · Accepted
Debug and release use the release keystore and the same application ID, so either installs over the other and keeps settings. They share one `versionCode` sequence.
*Why:* User wants to switch between debug and release builds without reinstalling or losing settings.
*Consequences:* A buggy debug build can affect real settings (back up first, once F13 exists); never install APKs built from older code (schemas only migrate forward); `keystore.properties` must exist on any machine that builds APKs for the phone.
*Alternative rejected:* `applicationIdSuffix ".debug"` (separate apps side by side).

**D-011 — Local diagnostics log from the first build** · 2026-10-15 · Accepted
F17: rotating on-device log files, crash reports, previous-exit reasons, and a Diagnostics screen with export. Built in Phase 0–1. No crash-reporting service and no network.
*Why:* So crashes and unexpected behaviour during early testing can be understood afterwards, including the phone killing background services.

**D-012 — All user-facing text in one resources module** · 2026-10-15 · Accepted
All text lives in `core/resources` (`strings.xml`, `plurals.xml`), reused across screens and enforced by Lint/detekt; colors and sizes come from `core/ui` tokens.
*Why:* Change text once and it updates everywhere; no duplicated or inconsistent wording.

---

## Pending (to be decided during the plan)
- Feasibility spike results (Step 0.5): app-clone visibility, usage-stats accuracy, service survival on the phone, F16 detection.
- Bundled fonts (Step 2.2): OFL-licensed only.
