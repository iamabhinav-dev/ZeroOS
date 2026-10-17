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
JDK 21 (originally 17; Robolectric needs 21 for API 37) + Android SDK command-line tools in the home folder; build with Gradle from the terminal; edit in VS Code; review UI through screenshot-test PNGs. Gradle memory is capped.
*Why:* The development machine has 8 GB RAM; Android Studio alone needs about that much.

**D-018 — Handling Play Protect install blocks** · 2026-10-15 · Accepted
From v1.1 (when the manifest declares a notification listener and an Accessibility service), install updates by briefly turning off "Scan apps with Play Protect", installing, and turning it back on. v1.0 needs nothing.
*Why:* In India, Play Protect blocks apps from browsers, messaging apps and file managers that declare these permissions; this workaround is free and needs no ADB or Developer options.
*Fallbacks:* Play Console internal testing track ($25, adds policy paperwork and Play signing); Google's free hobbyist developer verification once available in India.
*Confirmed 2026-10-16:* Play Protect blocked the spike APK on the test phone; choosing to install anyway in the Play Protect dialog worked, so pausing scanning may not even be needed.

**D-019 — Block screen sends you home, no grace period** · 2026-10-15 · Accepted
The F16 block button is "Go home" (`GLOBAL_ACTION_HOME`); the overlay reappears as soon as Shorts/Reels is detected again (only a ~1 s transition pause).
*Why:* Spike testing showed one Back press often stays on the Shorts tab; apps can't force-close other apps; a grace period would be a loophole.

**D-020 — Doto font for all text** · 2026-10-16 · Accepted (final)
Every piece of text in the launcher uses Doto (OFL, Google Fonts): clock, numbers, app names, lists, settings, dialogs, onboarding and stop screens. No font picker.
*Why:* User wants the Nothing Ndot 57 look everywhere; Ndot is proprietary, Doto is the free look-alike.
*Consequences:* Dot fonts are harder to read when small, so body text stays ≥ 14 sp and the text-size setting remains (NFR-A3/A4).

**D-021 — Random puns on stop screens** · 2026-10-15 · Accepted
The F16 black screen, the F8 mindful pause and the F8 limit-reached screen each show a random pun from their own built-in list by default (F16.9, F8.13); custom message (F16) or plain text (F8.14) optional.
*Why:* User's request; keeps the block screen light instead of preachy.

**D-022 — Dot animations on stop screens** · 2026-10-15 · Accepted
Breathing dots (pause), draining battery (limit reached), closing shutter (Shorts block); monochrome dot-matrix style; static when reduced motion is on.
*Why:* User chose small animations over per-pun pictograms.

**D-023 — Shorts blocker watches which app is in front** · 2026-10-15 · Accepted
The Accessibility service receives window changes from all apps (app name only) and reads content only from YouTube/Instagram; the overlay stays up through gaps under 1 s.
*Why:* Spike-2 testing: the overlay reappeared over Recents (it never learned the user had left YouTube) and blinked while Shorts loaded.

**D-024 — Phase 1 starts before spike results** · 2026-10-15 · Accepted
Phase 1 (home, app list, diagnostics, safe mode) starts while Step 0.5 phone testing is still running; Step 0.5 must be finished before Phase 2.
*Why:* None of the spike's open questions affect Phase 1; app-clone visibility (F2.10) is the earliest dependency, in Phase 2.

**D-025 — Diagnostics settings in SharedPreferences** · 2026-10-15 · Accepted
Logging on/off, log level and the last-seen crash report are stored in SharedPreferences, not DataStore.
*Why:* The logger reads them on every call and they're needed from Phase 1, before DataStore arrives in Phase 2. They're included in the settings backup (F13) when it's built.

**D-026 — Side swipes off by default; swipe down switchable** · 2026-10-16 · Accepted
Swipe left/right on home are optional and off by default; swipe down (search) is on by default with an on/off switch.
*Why:* The user uses side swipes for system Back and swipe-up-and-hold for Recents; home side swipes would clash.

**D-027 — No "swipe up" hint on home** · 2026-10-16 · Accepted
The swipe-up gesture is taught in the onboarding intro (G3) only; home stays clean.
*Why:* User's choice (wireframe question Q1).

**D-028 — Phase 2 starts with the spike mostly done** · 2026-10-16 · Accepted
Confirmed on the test phone: YouTube Shorts detection, overlay and Go home, usage stats similar to the phone's, services surviving a day of use and reconnecting after a restart. Remaining checks are due before the step that needs them: app clones before Step 2.4; overnight survival and Play Protect before Phase 4; Instagram Reels and a Shorts capture before Phase 6. Replaces the "finish Step 0.5 before Phase 2" rule of D-024.
*Why:* The confirmed results cover all Phase 2 risks except clone tags in the app list.

**D-029 — Settings stored as JSON in DataStore** · 2026-10-16 · Accepted
`LauncherSettings` is a kotlinx-serialization data class saved by DataStore as JSON (not Proto DataStore). Unknown fields are ignored, missing ones get Appendix A defaults, unknown enum values fall back to defaults, and a corrupted file is reset to defaults and logged.
*Why:* Same type safety as Proto DataStore without a protobuf code generator in the build; the settings backup (F13) is JSON anyway and can reuse the serializer.

**D-030 — Unknown second profiles are tagged "clone"** · 2026-10-16 · Superseded by D-034 for clones (work/private tags stay)
Apps from a profile Android reports as managed are tagged "work", private space "private", and every other non-main profile "clone". The raw profile type is logged once per profile.
*Why:* App Clone apps have the same name as the original, so they need a tag; the phone may not use Android's standard clone type, and App Clone is the only extra profile on this phone. The log shows the real type to refine this later.
*Confirmed 2026-10-16:* the test phone reports App Clone as Android's standard `android.os.usertype.profile.CLONE`, so the tag comes from the real type, not the fallback.

**D-031 — Dot-matrix charging bolt** · 2026-10-16 · Accepted
While charging, a lightning bolt drawn as dots on Doto's 5 × 7 grid follows the battery %. Drawn by a reusable `DotMatrixIcon` (also for the Phase 4/6 dot animations).
*Why:* User's request. Doto has no bolt glyph, and the ⚡ character would fall back to a colored emoji.

**D-032 — App icon: a round, dotted "0"** · 2026-10-16 · Superseded by D-037
The launcher icon is a "0" made of 14 white dots spaced evenly along an oval, on black, centred in the adaptive-icon safe zone, with a monochrome layer for themed icons.
*Why:* User wanted the Doto look; the first version used Doto's own 5 × 7 grid "0", which looked like a heptagon, so the dots now follow a smooth oval.

**D-033 — Swipe down: notifications later, no setting** · 2026-10-16 · Accepted
Swipe down opens search until the Accessibility service exists (Phase 4), then it expands the notification shade
while that service is on. The swipe-down on/off switch and action choice are removed from Settings. Updates D-026
and F4.4.
*Why:* Swipe up and swipe down both opened the app list. Opening the shade without Accessibility needs a hidden
Android API, which the project rules forbid.

**D-034 — Apps with the same name are numbered I, II, …** · 2026-10-16 · Accepted
When two or more visible apps have the same name (e.g. WhatsApp and its App Clone), they're shown as
"I-WhatsApp", "II-WhatsApp" everywhere: home, app list, pickers, onboarding, hidden apps and menus. The main
profile's app is always I; other profiles follow in a fixed order. Sorting and search use the plain name. Apps
with a custom name (F5.4) are shown exactly as named. Replaces the "· clone" tag of D-030.
*Why:* User's choice; the clone tag didn't show everywhere (e.g. on home), so clones looked identical.

**D-035 — No accent color** · 2026-10-16 · Accepted
F6.6 (optional single accent color) is removed; ZeroOS stays black and white (or white and black in the light
theme). Listed under Deferred Work.
*Why:* User's choice; it matches the no-colour rule and the approved Appearance wireframe.

**D-036 — Optional seconds on the home clock** · 2026-10-16 · Accepted
Settings → Appearance → Show seconds (off by default) shows the clock as hh:mm:ss (F6.7). The clock text
shrinks to fit one line. With seconds on, home updates once a second, but only while home is on screen;
otherwise it keeps using Android's once-a-minute tick (NFR-P5).
*Why:* User's request.

**D-037 — Logo: the dotted "ZeroOS" wordmark** · 2026-10-17 · Accepted
The user's logo (dotted "ZeroOS", slash removed from the zero) is the app icon, centred in the adaptive-icon
safe zone with a monochrome layer, and appears as a wordmark on the first onboarding page and at the top of
About, in the theme's text color. Source: `docs/brand/zeroos-logo.svg`, converted to Android vector drawables.
*Why:* User's choice after comparing four options on the phone (slashed zero, wordmark with and without slash).
Vectors stay sharp at every size. The word is small at icon size; the user accepted that.

**D-038 — Minimal wallpapers set from ZeroOS** · 2026-10-17 · Accepted (wireframe approved)
ZeroOS can set the phone's home and/or lock-screen wallpaper from a small monochrome gallery (F6.8), through
`WallpaperManager` only, with the `SET_WALLPAPER` permission. No lock-screen replacement, overlays,
Accessibility or OEM APIs. Pure black and Pure white are generated on the device; further images come from
`docs/brand/wallpapers/`, compressed to WebP once when added and stored in the app's assets, each named by one
string in `core/resources`. Each image is marked dark or light by its file name, and the gallery shows only
those matching the current theme (dark mode: dark ones; light mode: light ones; System follows the phone). The user picks home or lock; ZeroOS then always asks whether to use the same wallpaper on the other screen,
and leaves it untouched if not. "Restore previous" (per screen) covers wallpapers ZeroOS set: since Android 13 apps can't read
the current wallpaper without all-files access, which ZeroOS won't request.
*Why:* User's request; keeps the phone's look minimal from one place.

---

## Pending (to be decided during the plan)
- Remaining spike check (D-028), see `docs/spike/RESULTS.md`: overnight survival (before Phase 4).
