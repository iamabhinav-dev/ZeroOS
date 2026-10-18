# Wireframes

Low-fidelity layouts. They show **what goes where and what each interaction does**, not final colors or fonts.
Approved phase by phase (D-016). Agents build screens to match **approved** wireframes only.

| Section | Screens | Phase | Status |
|---|---|---|---|
| A | Home (final layout) | 0.4 | ✅ Approved 2026-10-15 |
| B | App list | 0.4 | ✅ Approved 2026-10-15 |
| C | Diagnostics | 0.4 | ✅ Approved 2026-10-15 |
| D | Safe mode | 0.4 | ✅ Approved 2026-10-15 |
| E | App actions: long-press menu, rename, favorites edit, hidden apps | 2.0 | ✅ Approved 2026-10-16 |
| F | Settings (home, app list, gestures, appearance, backup, about) | 2.0 | ✅ Approved 2026-10-16 |
| G | Onboarding | 2.0 | ✅ Approved 2026-10-16 |

Legend: `[ text ]` = button · `▁▃▅` = chart · `‹…›` = placeholder value · *italic notes* = behavior.

---

## A. Home (final layout, all phases)

The home screen never scrolls (F1.6). It is built in Phase 2 with **empty slots** for later features; an empty slot takes no space.

### A1. Full layout (all features on)
```
┌────────────────────────────────────┐
│ status bar (optional, F6.5)        │
│                                    │
│  10:42                             │  ① clock      tap → alarms (F1.1)
│  Thursday, 15 October · 82%        │  ② date · battery   tap date → calendar (F1.2, F1.3)
│  ⏰ 6:30                            │  ③ next alarm (optional, F1.8)
│                                    │
│  2h 14m · 38 unlocks               │  ④ screen-time stats slot   tap → Insights (F10.3)
│                                    │
│  focus                             │  ⑤ focus slot   tap → focus picker (F9.1)
│                                    │
│  ○ Dentist 4:30 pm                 │  ⑥ calendar slot (F12.2, off by default)
│  ☐ buy milk                        │  ⑦ to-do slot (F12.3, off by default)
│  ☐ call mom                        │
│                                    │
│                                    │  ← flexible empty space
│                                    │
│  Phone                             │
│  Messages                          │  ⑧ favorites (F1.4), 0–10 apps
│  Camera                            │     tap → open app
│  Maps                              │
│  WhatsApp                          │
│  Spotify                           │
│                                    │
└────────────────────────────────────┘
```
*Gestures (anywhere on empty space):*
- *swipe up → app list (F4.1) · swipe left / right → an app, only if turned on (off by default, F4.2, F4.3, D-026)*
- *swipe down → search; notification shade when Accessibility is on (F4.4, D-033)*
- *double-tap → lock (only when Accessibility is on, F4.5)*
- *long-press empty space → Settings (F1.9)*

*Layout rules:*
- Top block (①–⑦) sits at the top; favorites (⑧) sit in the lower half, within thumb reach. Empty space between them absorbs size changes.
- Favorites alignment left / center / right applies to ⑧ and the top block together (F1.7). Shown: left.
- **Favorites are always fully visible.** If space runs out (e.g., largest font), slots shrink in this order: ⑦ to-do → ⑥ calendar → ④ chart falls back to Text style → ③ next alarm hidden.

### A2. Minimal (v1.0 defaults: nothing optional on)
```
┌────────────────────────────────────┐
│                                    │
│  10:42                             │
│  Thursday, 15 October · 82%        │
│                                    │
│                                    │
│                                    │
│                                    │
│                                    │
│  Phone                             │
│  Messages                          │
│  Camera                            │
│  Maps                              │
│                                    │
└────────────────────────────────────┘
```

### A3. Screen-time stats slot ④: the four styles (F10.5)
Each is one to three lines tall. All monochrome.

**Text** (default)
```
│  2h 14m · 38 unlocks               │
│  ↓ 12% vs yesterday                │  (optional, F10.8)
```

**Progress bar** (goal 4h, F10.7)
```
│  2h 14m / 4h · 38 unlocks          │
│  ▓▓▓▓▓▓▓▓▓░░░░░░░                  │
```
Over the goal:
```
│  4h 23m · over by 23m · 61 unlocks │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                  │
```

**Weekly bars** (today = last bar, drawn solid; others outlined)
```
│  ▂ ▅ ▃ ▇ ▄ ▃ █   2h 14m            │
│  F S S M T W T   38 unlocks        │
```

**Hourly timeline** (24 thin bars, 00–23 h)
```
│  ▁ ▁ ▃▅▂▁▄▆▃▂▁▅▇▂                  │
│  0      6     12     18    now     │
│  2h 14m · 38 unlocks               │
```

**Usage Access not granted yet** (F10.11)
```
│  Tap to show screen time       ×   │  × dismisses permanently
```

### A4. Focus slot ⑤ states (F9)
```
Idle:     │  focus                             │   tap → focus picker (wireframed in Phase 4)
Active:   │  focus · 23m left                  │   hold 3 s → end early (F9.5)
Hidden:   (slot takes no space; setting F9.1)
```
While focus is active, favorites that aren't on the allow-list are hidden (F9.3).

### A5. Largest font size check
At the largest system font × XL text size, with all slots filled:
```
┌────────────────────────────────────┐
│  10:42                             │
│  Thu, 15 Oct · 82%                 │  date shortens to fit
│  2h 14m · 38 unlocks               │  chart → Text style
│  focus                             │
│                                    │  calendar & to-do hidden
│  Phone                             │
│  Messages                          │
│  Camera                            │
│  Maps                              │
│  WhatsApp                          │
│  Spotify                           │
└────────────────────────────────────┘
```

---

## B. App list (F2)
Opened by swiping up from home.

### B1. Default (keyboard open, F2.3)
```
┌────────────────────────────────────┐
│  search…                           │  search field, focused
│  ────────────────────────────────  │
│  Calculator                       A│
│  Calendar                         B│
│  Camera                           C│  fast-scroller (optional, off by default, F2.7)
│  Chrome                           D│
│  Clock                            …│
│  Contacts                          │
│  Drive                             │
│  Gmail                             │
│  Instagram          12m left       │  remaining limit (from v1.1, F8.5)
│  ─────────── keyboard ──────────── │
└────────────────────────────────────┘
```
*tap → open app · long-press → app menu (wireframed in Phase 2) · back or swipe down → home (F2.11)*

### B2. Searching "gm"
```
│  gm                                │
│  ────────────────────────────────  │
│  Gmail                             │  substring match
│  Google Maps                       │  initials match (F2.4)
```
*Exactly one match + auto-launch on (F2.5, off by default) → opens it immediately.*

### B3. No match (F2.6)
```
│  xyzq                              │
│  ────────────────────────────────  │
│  Search the web for "xyzq"         │
│  Search Play Store for "xyzq"      │
```

### B4. Apps from other profiles (F2.10)
```
│  WhatsApp                          │
│  WhatsApp · clone                  │
│  Gmail · work                      │
```

---

## C. Diagnostics (F17.9)
Phase 1: opened by long-pressing empty space on home. From Phase 2: Settings → Diagnostics.

### C1. Log list
```
┌────────────────────────────────────┐
│  ← Diagnostics                     │
│                                    │
│  [ All ] [ Info ] [ Warn ] [ Error ]│  level filter
│                                    │
│  10:42:03  I  home   shown         │
│  10:41:58  I  apps   launched      │
│                       com.whatsapp │
│  10:40:12  W  data   slow read 82ms│
│  10:12:44  E  focus  timer failed ›│  tap → full entry with stack trace
│  …                                 │
│                                    │
│  Crash reports (2)               › │  → C2
│  Last stop: killed by system,      │
│  10:05 (Android 11+, F17.6)        │
│                                    │
│  [ Export logs ]   [ Clear logs ]  │
│                                    │
│  Logging          on               │  F17.11
│  Level            INFO             │
└────────────────────────────────────┘
```
*Export → zip via share sheet or save to file. Clear → confirmation dialog.*

### C2. Crash reports
```
│  ← Crash reports                   │
│                                    │
│  15 Oct 10:05  NullPointerExc…   › │  tap → full report (stack trace + last 50 entries)
│  14 Oct 22:31  IllegalStateEx…   › │
```

### C3. After a crash (next start, F17.10)
```
│  ZeroOS crashed last time.         │
│  [ View report ]   [ Dismiss ]     │  small banner at the top of home, once
```

---

## D. Safe mode (F18)
Shown after 3 crashes within 60 s of starting. Reads no settings and starts no services.

```
┌────────────────────────────────────┐
│  Safe mode                         │
│  ZeroOS crashed repeatedly.        │
│                                    │
│  [ View crash report ]             │
│  [ Open Home settings ]            │  switch to the stock launcher
│  [ Exit safe mode ]                │  reset counter, restart normally
│  ────────────────────────────────  │
│  Calculator                        │
│  Calendar                          │  plain alphabetical list
│  Camera                            │  tap → open app
│  Chrome                            │  (no search, gestures, renames,
│  Clock                             │   hidden apps or limits)
│  …                                 │
└────────────────────────────────────┘
```
Note: because safe mode doesn't read settings, hidden apps appear and renamed apps show their original names.

---

## E. App actions (F2.8, F3, F5)

### E1. Long-press menu on an app (bottom sheet, F2.8)
Opened by long-pressing an app in the app list, or a favorite on home.
```
┌────────────────────────────────────┐
│  …app list dimmed behind…          │
├────────────────────────────────────┤
│  Instagram                         │  app name (custom name if renamed)
│  ────────────────────────────────  │
│  Add to favorites                  │  "Remove from favorites" if already one
│  Rename                            │  → E2
│  Hide                              │  hides from list and search (F5.1)
│  App info                          │  opens Android's app info page
│  Uninstall                         │  opens Android's uninstall dialog
└────────────────────────────────────┘
```
*"Set limit" is added here in Phase 4 (F8). Swipe down or Back closes the sheet.*

### E2. Rename (F5.4, F5.5)
```
┌────────────────────────────────────┐
│  Rename Instagram                  │
│                                    │
│  Time Sink▏                        │  text field, keyboard open
│  ────────────────────────────────  │
│                                    │
│  [ Reset to original ]             │  only shown if renamed
│                  [ Cancel ] [ Save ]│
└────────────────────────────────────┘
```
*Empty name isn't allowed (Save disabled). Max 40 characters.*

### E3. Favorites edit mode (F3.1–F3.3)
Settings → Home → Edit favorites.
```
┌────────────────────────────────────┐
│  ← Favorites            4 / 6      │  count / max (F3.3)
│                                    │
│  ≡  Phone                     ✕    │  ≡ drag handle: hold and drag to reorder
│  ≡  Messages                  ✕    │  ✕ removes from favorites
│  ≡  Camera                    ✕    │
│  ≡  Maps                      ✕    │
│                                    │
│  + Add app                         │  → app list in "pick" mode
└────────────────────────────────────┘
```
*"+ Add app" is hidden when the maximum is reached.*

### E4. Hidden apps (F5.2, F5.3)
Settings → App list → Hidden apps.
```
┌────────────────────────────────────┐
│  ← Hidden apps                     │
│                                    │
│  Instagram               [ Unhide ]│
│  YouTube                 [ Unhide ]│
│                                    │
│  Hidden apps are not shown in the  │
│  app list or search.               │
└────────────────────────────────────┘
```
*If "Hidden apps confirmation" is set (F5.3): before this list opens, either a confirm dialog
("Show hidden apps?" [Cancel] [Show]) or a 5-second countdown screen ("Opening in 5…", [Cancel]).*

---

## F. Settings (F6, F2/F3/F4/F5 options, F13)

Opened by long-pressing empty space on home (F1.9). Plain text rows; tap a row to change it.
Values in `‹…›` show the current setting (defaults from REQUIREMENTS.md Appendix A).

### F1. Settings main
```
┌────────────────────────────────────┐
│  ← Settings                        │
│                                    │
│  Home                            › │
│  App list                        › │
│  Gestures                        › │
│  Appearance                      › │
│  Backup                          › │
│  Diagnostics                     › │  → C1 (existing)
│  About                           › │
└────────────────────────────────────┘
```
*Wellbeing, Notifications, Short-form blocker and Grayscale sections are added in later phases.*

### F2. Home
```
│  ← Home                            │
│  Edit favorites                  › │  → E3
│  Maximum favorites          ‹6›    │  1–10
│  Alignment              ‹Left›     │  left / center / right (F1.7)
│  Show battery %            ‹on›    │  (F1.3)
│  Show next alarm           ‹off›   │  (F1.8)
```

### F3. App list
```
│  ← App list                        │
│  Open keyboard automatically ‹on›  │  (F2.3)
│  Open single match automatically ‹off›│ (F2.5)
│  Alphabet scroller         ‹off›   │  (F2.7)
│  Hidden apps (2)                 › │  → E4
│  Hidden apps confirmation  ‹off›   │  off / confirm / 5 s delay (F5.3)
```

### F4. Gestures
```
│  ← Gestures                        │
│  Swipe left          ‹off›       › │  off / pick an app (F4.2, F4.6); off by default (D-026)
│  Swipe right         ‹off›       › │  off / pick an app (F4.3)
│  Double-tap to lock  ‹needs Accessibility (v1.1)› │  disabled until Phase 4 (F4.5)
```
*Swipe up is always "App list" (F4.1) and swipe down is always search, or the notification shade once
Accessibility is on (F4.4, D-033), so neither is shown here. Side swipes start away from the screen edges so the
system Back gesture still works.*

### F5. Appearance
```
│  ← Appearance                      │
│  Theme               ‹Dark›        │  dark (pure black) / light / system (F6.1)
│  Home background     ‹Black›       │  black / phone wallpaper (dimmed) (F6.2); was "Wallpaper"
│  Phone wallpaper                 › │  → F5b (F6.8), proposed
│  Text size           ‹Medium›      │  S / M / L / XL (F6.4)
│  Show clock          ‹on›          │  (F6.5)
│  Show seconds        ‹off›         │  hh:mm:ss; only while the clock is shown (F6.7)
│  Show date           ‹on›          │
│  Show status bar     ‹on›          │
│                                    │
│   10:42                            │  live preview of home with the current settings
│   Thursday, 15 October · 82%       │
│   Phone                            │
```

### F5b. Phone wallpaper (F6.8) — approved 2026-10-17 (revised: full-screen preview)
```
┌────────────────────────────────────┐
│  ← Phone wallpaper                 │
│                                    │
│  ┌──────┐  ┌──────┐  ┌──────┐      │  thumbnails in the phone's shape,
│  │██████│  │█·██·█│  │██████│      │  3 per row, scrolls if more;
│  │██████│  │██··██│  │······│      │  tap one → full-screen preview (F5c)
│  │██████│  │█·██·█│  │██████│      │
│  └──────┘  └──────┘  └──────┘      │
│  Pure black  Rose       Great wave │  name under each (string per image)
│                                    │  dark mode: dark ones only; light mode:
│                                    │  light ones only (Pure white instead of
│                                    │  Pure black); System follows the phone
│                                    │
│  Home: Rose · Lock: not set by ZeroOS │ muted; what ZeroOS last set on each
│  Wallpaper set on the home screen  │  result line after Apply, or the error
│                                    │
│  Restore previous home wallpaper › │  only when ZeroOS has set two on home
│  Restore previous lock wallpaper › │  only when ZeroOS has set two on lock
│                                    │
│  Android doesn't let apps read the │  always shown, muted (D-038)
│  current wallpaper, so ZeroOS can  │
│  only restore wallpapers it set.   │
└────────────────────────────────────┘
```

### F5c. Wallpaper preview (F6.8)
```
┌────────────────────────────────────┐
│  ←                                 │  dotted back arrow (Doto has no ←);
│                                    │  Back gesture does the same
│                                    │
│            (wallpaper,             │  full screen, cropped exactly as it
│             full screen)           │  will be on the phone; swipe left/right
│                                    │  for the next/previous one (stops at the
│                                    │  first and last, no wrapping)
│                                    │
│                Rose                │  name
│               Apply                │  → dialog below
└────────────────────────────────────┘

┌──────────────────────────┐
│  Set wallpaper on        │
│                          │
│  Home screen             │  only the home screen
│  Lock screen             │  only the lock screen
│  Both                    │  home and lock
└──────────────────────────┘
```
*Tapping outside the dialog or Back closes it without changes. After a choice, the preview closes and the
gallery shows the result line. When "lock" is included and the phone rejects a separate lock-screen wallpaper,
the result line says so; some phones (the phone's Themes app) can still override the lock screen, so the line points
there. Setting the home wallpaper while Home background is "Black" switches it to "Phone wallpaper" and says so.*

### F6. Backup (F13)
```
│  ← Backup                          │
│  Export settings                 › │  → Android "save file" (zeroos-settings-<date>.json)
│  Import settings                 › │  → Android "open file", then confirm:
│                                    │    "Replace all settings with this backup?" [Cancel] [Import]
│  Last export: 15 Oct, 10:42        │
```
*Import shows an error line if the file isn't a ZeroOS backup or is from a newer version.*

### F7. About
```
│  ← About                           │
│  ZeroOS (dotted wordmark)          │  logo, theme text color (D-037)
│  ZeroOS 0.1.0 (12)                 │  versionName (versionCode)
│  Open-source licenses            › │  fonts (OFL) and libraries
```

---

## G. Onboarding (F14)
Shown once on first start; "Skip" on every page goes straight to home (F14.5).

### G1–G3. Intro (F14.1)
```
┌────────────────────────────────────┐
│                          [ Skip ]  │
│                                    │
│   ●○○                              │  page dots (Doto style)
│                                    │
│   ZeroOS (dotted wordmark)         │  G1 only (D-037)
│   Less phone.                      │  G1: "Less phone. More life."
│   More life.                       │  G2: "Just words. No icons, no badges, no noise."
│                                    │  G3: "Swipe up for apps. Long-press for settings."
│                                    │
│                         [ Next ]   │
└────────────────────────────────────┘
```

### G4. Set as default (F14.2, F18.7)
```
│   Make ZeroOS your home screen     │
│                                    │
│   Press Home anytime to come back  │
│   here.                            │
│                                    │
│   Keep the stock launcher installed:  │
│   it's your fallback if anything   │
│   goes wrong.                      │
│                                    │
│   [ Set as default ]   [ Later ]   │  Android's "default home app" dialog (RoleManager)
```
*If ZeroOS is already the default, this page shows "ZeroOS is your home screen ✓" and [ Next ].*

### G5. Pick favorites (F14.3)
```
│   Pick your favorites   3 / 6      │
│                                    │
│   ☑ Phone                          │  pre-selected suggestions
│   ☑ Messages                       │
│   ☑ Camera                         │
│   ☐ Maps                           │
│   ☐ Calculator                     │  then all other apps, alphabetical
│   …                                │
│                         [ Next ]   │
```

### G6. Keep ZeroOS running (F14.6)
```
│   Keep ZeroOS running              │
│                                    │
│   Your phone may stop apps in the  │
│   background to save battery.      │
│                                    │
│   Battery optimization: ‹on›       │
│   [ Turn off for ZeroOS ]          │  Android's "ignore battery optimization" dialog
│                                    │
│   On some phones, also allow background   │  plain-text tip, no device-specific code
│   activity for ZeroOS in its app   │
│   settings.                        │
│                                    │
│                         [ Done ]   │
```

---

## H. Phase 4: wellbeing, focus, insights (F8–F10) — approved 2026-10-17 (notifications H9 deferred, D-039)

Same rules as before: Doto everywhere, black and white only, no icons or badges, text rows; values in `‹…›`
are Appendix A defaults. Everything in this phase is **opt-in** and works with every permission denied.
Home's screen-time slot ④ and focus slot ⑤ are already drawn in A3 and A4.

### H1. Settings main (additions)
```
│  ← Settings                        │
│  Home                            › │
│  App list                        › │
│  Gestures                        › │
│  Appearance                      › │
│  Wellbeing                       › │  → H3 (F8)        new
│  Screen time                     › │  → H4 (F10)       new
│  Focus                           › │  → H10 (F9)       new
│  Backup                          › │
│  Diagnostics                     › │
│  About                           › │
```
*Screen time is its own page, not under Wellbeing: home stats are on by default (F10.3) while Wellbeing is off
by default (F8.8), so they mustn't share one master switch.*

### H2. Permission explanation (one template for all four, F14.4, NFR-S4)
Shown only when a feature that needs it is switched on, never up front.
```
│  ← Usage access                    │  title = the permission
│                                    │
│  ZeroOS needs usage access to show │  why, in one or two lines
│  your screen time and to know how  │
│  long you've used an app today.    │
│                                    │
│  It stays on this phone. Nothing   │  privacy line (NFR-S2)
│  is sent anywhere.                 │
│                                    │
│  Open settings                   › │  → Android's screen for it
│  Not now                         › │  → back; the feature stays off (fallback)
```
On return ZeroOS checks again: granted → the feature turns on; still missing → "Not allowed yet" under the
title and the feature stays off.

Extra lines per permission:
- **Notification access / Accessibility** (sideloaded app, Android 13+), numbered steps:
```
│  If Android says "Restricted       │
│  setting":                         │
│  1  Open App info for ZeroOS       │  → App info
│  2  Tap ⋮ (top right)              │  the only symbol not drawn in Doto:
│  3  Allow restricted settings      │  it names Android's own button
│  4  Come back and try again        │
```
- **Accessibility** also says what it reads and the known side effect:
```
│  It sees which app is in front and │  (F8.7, later F16; D-023)
│  reads nothing else.               │
│                                    │
│  Some banking/UPI apps warn while  │  spike result (docs/spike/RESULTS.md)
│  any accessibility service is on.  │
```
- **Do Not Disturb** (F9.7): "Lets Focus turn on Do Not Disturb and back off."

### H3. Settings → Wellbeing (F8)
```
│  ← Wellbeing                       │
│  Wellbeing                 ‹off›   │  master (F8.8); off hides every row below
│                                    │  and stops all tracking; settings are kept
│  Mindful pause             ‹on›    │  (F8.1, F8.9)
│  Pause length              ‹5 s›   │  3–10 s
│  Intention prompt          ‹off›   │  (F8.2)
│  Daily limits              ‹on›    │  needs Usage access → H2 (F8.3)
│  Apps                            › │  → H5: which apps pause / have limits
│  "5 more minutes"          ‹2/day› │  0–5 (F8.4)
│  Also outside ZeroOS       ‹off›   │  needs Accessibility → H2 (F8.7)
│  Turning off               ‹hold 3 s› │ off / hold 3 s / pause (F8.12)
```
*Turning Wellbeing off while a limit is active asks for the chosen friction first (H7c).*

### H4. Settings → Screen time (F10)
```
│  ← Screen time                     │
│  Show on home              ‹on›    │  (F10.3); needs Usage access → H2
│  Style                     ‹Text›  │  text / progress bar / weekly bars /
│                                    │  hourly timeline (F10.5, drawn in A3)
│  Daily goal                ‹off›   │  off / 30 min … 8 h, step 30 min (F10.7)
│  Compare with yesterday    ‹off›   │  (F10.8)
│  Insights                        › │  → H12
```

### H5. Wellbeing → Apps (F8.1, F8.3)
```
│  ← Apps                            │
│  search…                           │
│  Instagram       pause · 30 min    │  tap → H5a; only apps with a rule listed
│  YouTube         30 min            │  first, then all others (no rule) below
│  Chrome          pause             │
│  ────────────────────────────────  │
│  Calculator                        │
│  Camera                            │
```
**H5a. One app** (bottom sheet, like E1)
```
│  Instagram                         │
│  Mindful pause             ‹on›    │
│  Daily limit               ‹30 min› │ off / 5 … 240 min (F8.3)
```

### H6. Mindful pause (F8.1, F8.2, F8.13, F8.15)
Full screen, shown before a flagged app opens from ZeroOS.
```
┌────────────────────────────────────┐
│                                    │
│               ·  ·  ·              │  breathing dots, grow and shrink with
│             ·  ●  ●  ·             │  the countdown (static with reduced
│               ·  ·  ·              │  motion, NFR-A4)
│                                    │
│     Is this a want, or a habit?    │  random line from the pause list
│                                    │
│                 4                  │  countdown (5 s default)
│                                    │
│  Why are you opening it?           │  only if Intention prompt is on;
│  ________________________________  │  never saved or logged (F17.8)
│                                    │
│  Go back            Open Instagram │  "Open …" appears when the countdown ends
│                                    │  (and, with the prompt on, once an intention is typed, D-043)
└────────────────────────────────────┘
```

### H7. Limit reached (F8.4, F8.13, F8.15)
```
┌────────────────────────────────────┐
│                                    │
│            ┌───────────┐           │  dot battery draining to empty, once
│            │ ● ● · · · │▌          │  (static with reduced motion)
│            └───────────┘           │
│                                    │
│       That's a wrap for today.     │  random line from the limit list
│   Instagram · 30 min used today    │  (always a pun, D-045)
│                                    │
│  Go home        5 more minutes (1) │  (1) = uses left today; hidden at 0
└────────────────────────────────────┘
```
**H7b.** The same screen appears over other apps when "Also outside ZeroOS" is on (F8.7, Accessibility).
**H7c. Turning off during a limit** (F8.12): "Hold to turn off" button that fills over 3 s, or the pause (H6).

### H14. Take a break (F8.16–F8.18), approved 2026-10-18
Over a chosen app after 15 min straight (Accessibility overlay).
```
┌────────────────────────────────────┐
│             ·  ·  ·                │  breathing dots
│           ·  ●  ●  ·               │
│             ·  ·  ·                │
│                                    │
│  Your attention span called.       │  list D
│  It wants a break.                 │
│                                    │
│        [ Just one more video… ]    │  list E; after tapping it the label
│                                    │  becomes "5", "4" … then the screen
│                                    │  goes away. Back does nothing.
└────────────────────────────────────┘
```
Settings → Wellbeing gains "Take a break ‹off›", "After ‹15 min›", "Wait after tapping ‹5 s›"; the per-app sheet
(H5a) gains "Take a break ‹off›".

### H8. App list with limits (F8.5)
```
│  Instagram                  12m left │  small muted text, only for limited apps
│  Maps                              │
│  YouTube                    used up │  tap → H7
```

### H10. Settings → Focus (F9)
```
│  ← Focus                           │
│  Show "focus" on home      ‹on›    │  (F9.1)
│  Start focus                     › │  → H11
│  Turn on Do Not Disturb    ‹off›   │  needs DND access → H2 (F9.7)
│  Schedules                       › │  → H10a (F9.6)
```
**H10a. Schedules**
```
│  ← Schedules                       │
│  Weekdays 09:00–12:00     ‹on›     │  tap → edit days, start, end, apps
│  Add schedule                    › │
```

### H11. Focus picker (F9.2), opened by "focus" on home or Settings
```
┌────────────────────────────────────┐
│  ← Focus                           │
│  How long?                         │
│  25 min     50 min     90 min      │  tap one (selected = outlined)
│  Custom                  ‹45 min›  │  5–240 min
│                                    │
│  Apps you can use                  │  picked apps show a number, like
│  Phone                          1  │  onboarding (Doto has no ☑)
│  Messages                       2  │
│  Maps                              │
│  …                                 │
│                                    │
│  Start focus                       │
└────────────────────────────────────┘
```
While active: home slot ⑤ shows "focus · 23m left" (A4); favorites and the app list show only the allowed
apps (F9.3); Do Not Disturb keeps it quiet if turned on (F9.7; notification filtering waits for F7, D-039);
**holding** "focus · 23m left" for 3 s ends it early, with a filling dot line as feedback (F9.5). The picker
remembers the last allowed apps as the default for next time.

### H12. Insights (F10.1, F10.2), from home stats or Screen time
```
┌────────────────────────────────────┐
│  ← Insights                        │
│  Today                             │
│  2h 14m                            │  big number
│  38 unlocks · ↓ 12% vs yesterday   │  comparison only if turned on
│                                    │
│  Top apps                          │
│  YouTube      ●●●●●●●●  52m        │  dot bars, longest = full width
│  WhatsApp     ●●●●●     31m        │
│  Instagram    ●●●       18m        │
│  Chrome       ●●        12m        │
│  Maps         ●          6m        │
│                                    │
│  Last 7 days                       │  dot columns, today solid,
│  ● ●   ● ●                         │  other days hollow (outline dots)
│  ● ● ● ● ● ●                       │
│  ● ● ● ● ● ● ●                     │
│  F S S M T W T                     │
└────────────────────────────────────┘
```
*Every chart has a spoken description with its values (F10.13). Android 8: unlocks hidden (F10.12).*

### H13. Gestures page after Phase 4
```
│  Double-tap to lock        ‹off›   │  enabled once Accessibility is on (F4.5);
│                                    │  otherwise opens H2 for Accessibility
```
*Swipe down opens the notification shade automatically while Accessibility is on (F4.4, D-033).*

### Answers for H (2026-10-17)
- Pause and limits share one Apps list (H5).
- Focus remembers the last allowed apps as the default.

### H9 (deferred, D-039). Notifications and inbox
Kept for when F7 is built; not part of v1.1.

#### Settings → Notifications (F7)
```
│  ← Notifications                   │
│  Filter notifications      ‹off›   │  needs Notification access → H2 (F7.1)
│  Allowed apps (4)                › │  → app picker; their notifications come
│                                    │  through as usual (F7.2)
│  Digest                    ‹off›   │  (F7.4)
│  Digest times        ‹12:00, 18:00› │ tap → time picker list
│  Inbox (12)                      › │  → Inbox below
│                                    │
│  Calls, alarms and system alerts   │  always shown, muted (F7.6)
│  always come through.              │
```
**Inbox** (F7.3, F7.5)
```
│  ← Inbox                           │
│  WhatsApp · 3                      │  grouped by app, newest first
│    Rahul: on my way          10:41 │  tap → opens it (if Android still allows)
│    Mom: call me              10:02 │  long-press → dismiss
│  Gmail · 1                         │
│    Your order shipped         9:15 │
│                                    │
│  Clear all                         │  asks to confirm
```
*Opened from here and from the digest notification. Home shows no badge or count (F1.5).*

## Answered questions
- **Q1. "Swipe up for apps" hint on home:** no; taught in intro page G3 only (D-027).
- **Q2. Fonts:** Doto for all text, no font picker (D-020).
- **Gestures:** swipe left/right off by default; swipe down on by default with a switch (D-026).

