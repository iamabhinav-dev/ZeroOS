# Step 0.5 — Feasibility spike results

Device: **the test phone**. Trial app `zeroos-spike-3`. Data: exported logs of 15 Oct 14:14–23:49 and 16 Oct 00:04 (after a restart).
The raw log isn't committed (it lists which apps were used); only YouTube screen-structure dumps are kept.

| # | Check | Result | Status |
|---|---|---|---|
| 1 | App list (`LauncherApps`), app clones | App Clone apps appear as a separate profile with the **same name** as the original (the stock launcher shows them as "II-<name>"), so ZeroOS must tag them (F2.10) | ✅ |
| 2 | Usage stats vs the phone's screen time | Reported as similar to the phone's own screen time | ✅ |
| 3 | Notification listener survives | Connected once at 14:15, received notifications until 23:49 (~9.5 h, incl. gaming), never disconnected. After a restart at 00:03 it reconnected on its own within a second | ✅ Same day + restart · ⏳ overnight |
| 4 | Accessibility service survives | Connected once at 14:15, events until 23:49, never unbound. Reconnected on its own right after the restart | ✅ Same day + restart · ⏳ overnight |
| 4 | YouTube Shorts detection | `reel_recycler`, `reel_player_page_container`, `reel_watch_player` present on every Shorts screen, absent on all other YouTube screens; lookups 1–79 ms | ✅ |
| 4 | Normal YouTube not blocked | Evening use in "Shorts only" mode: many normal screens, no overlay | ✅ |
| 4 | Instagram Reels detection | Instagram not opened during the test | ⏳ Not tested |
| 5 | Black overlay | Shown 132–705 ms after the event (most under 250 ms); "Go home" works; no overlay over Recents/launcher after leaving YouTube | ✅ |
| 6 | Play Protect install block | **Blocked** the spike install (it declares notification-listener and Accessibility services); installing anyway from the Play Protect dialog worked | ✅ Workaround confirmed |

Other findings:
- The phone's default launcher package is `com.android.launcher3`; `com.example.simplelauncher` also exists.
- Overlay latency target is ≤ 300 ms (F16 budget); one case took 705 ms, to watch in Phase 6.
- `youtube-normal-screens/`: 30 screen-structure dumps of non-Shorts YouTube screens (view IDs, bounds, no text),
  kept as "must not match" test data for Phase 6. A Shorts dump still needs capturing (the trial app keeps only
  the last 30 dumps, so the Shorts ones were rotated out).
