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
*Consequences:* A buggy debug build can affect real settings (back up first with F13, available from v1.0); never install APKs built from older code (schemas only migrate forward); `keystore.properties` must exist on any machine that builds APKs for the phone.
*Alternative rejected:* `applicationIdSuffix ".debug"` (separate apps side by side).

**D-011 — Local diagnostics log from the first build** · 2026-10-15 · Accepted
F17: rotating on-device log files, crash reports, previous-exit reasons, and a Diagnostics screen with export. Built in Phase 0–1. No crash-reporting service and no network.
*Why:* So crashes and unexpected behaviour during early testing can be understood afterwards, including the phone killing background services.

**D-012 — All user-facing text in one resources module** · 2026-10-15 · Accepted
All text lives in `core/resources` (`strings.xml`, `plurals.xml`), reused across screens and enforced by Lint/detekt; colors and sizes come from `core/ui` tokens.
*Why:* Change text once and it updates everywhere; no duplicated or inconsistent wording.

**D-013 — Off-machine backups for code and keystore** · 2026-10-15 · Accepted
Code goes to a private GitHub repo (push `main` after each step, with approval); the keystore is backed up separately off this computer, never in git.
*Why:* Losing this computer would otherwise lose the code and the only key that can update the installed app.

**D-014 — Settings backup in v1.0** · 2026-10-15 · Accepted
F13 export/import moves from v1.2 to v1.0 (Step 2.10); each later feature adds its settings to the backup format.
*Why:* Debug builds install over the everyday app (D-010), so a backup must exist from the start.

**D-015 — Focus starts from a home button** · 2026-10-15 · Accepted
A text button "focus" on home starts Focus mode; it shows remaining time while active.
*Why:* User's choice; one tap is quicker than Settings or a hidden long-press.

**D-016 — Wireframes approved phase by phase** · 2026-10-15 · Accepted
Each phase starts with a wireframe step (0.4, 2.0, 4.0, 5.0, 6.0); the home screen is drawn in its final form up front.
*Why:* Less review up front, and later screens benefit from using earlier versions. The home screen is the exception because it can't scroll and must fit everything.

**D-017 — Command-line toolchain, no Android Studio** · 2026-10-15 · Accepted
JDK 17 + Android SDK command-line tools in the home folder; build with Gradle from the terminal; edit in VS Code; review UI through screenshot-test PNGs. Gradle memory is capped.
*Why:* The development machine has 8 GB RAM; Android Studio alone needs about that much.

**D-018 — Handling Play Protect install blocks** · 2026-10-15 · Accepted
From v1.1 (when the manifest declares a notification listener and an Accessibility service), install updates by briefly turning off "Scan apps with Play Protect", installing, and turning it back on. v1.0 needs nothing.
*Why:* In India, Play Protect blocks apps from browsers, messaging apps and file managers that declare these permissions; this workaround is free and needs no ADB or Developer options.
*Fallbacks:* Play Console internal testing track ($25, adds policy paperwork and Play signing); Google's free hobbyist developer verification once available in India. Re-check during spike Step 0.5.

---

## Pending (to be decided during the plan)
- Feasibility spike results (Step 0.5): app-clone visibility, usage-stats accuracy, service survival on the phone, F16 detection.
- Bundled fonts (Step 2.2): OFL-licensed only.
