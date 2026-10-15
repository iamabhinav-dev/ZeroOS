# Wireframes

Low-fidelity layouts. They show **what goes where and what each interaction does**, not final colors or fonts.
Approved phase by phase (D-016). Agents build screens to match **approved** wireframes only.

| Section | Screens | Phase | Status |
|---|---|---|---|
| A | Home (final layout) | 0.4 | ✅ Approved 2026-10-15 |
| B | App list | 0.4 | ✅ Approved 2026-10-15 |
| C | Diagnostics | 0.4 | ✅ Approved 2026-10-15 |
| D | Safe mode | 0.4 | ✅ Approved 2026-10-15 |

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
- *swipe up → app list (F4.1) · swipe left → Camera (F4.2) · swipe right → Phone (F4.3)*
- *swipe down → search, or notification shade when Accessibility is on (F4.4)*
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
