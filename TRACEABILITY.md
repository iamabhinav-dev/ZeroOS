# Requirement → Test Traceability

Every implemented requirement ID must map to at least one automated test, or to a manual phone check when automation isn't possible on the JVM.
Update this file in the same commit as the code and tests.

**Status:** ⬜ not started · 🟡 in progress · ✅ implemented & tested · ❌ removed

| ID | Status | Automated tests (class › test) | Manual check |
|---|---|---|---|
| F1.1 | 🟡 | ClockFormatTest › f1_1_*, TimeTicksTest › f1_1_*, HomeScreenTest › f1_1_f1_2_showsClockAndDate (tap → alarms in Step 2.3) | Clock updates each minute |
| F1.2 | 🟡 | ClockFormatTest › f1_2_formatsWeekdayDayAndMonth, HomeScreenTest › f1_1_f1_2_showsClockAndDate (tap → calendar in Step 2.3) | |
| F1.3–F1.9 | ⬜ | | |
| F2.1, F2.3–F2.8 | ⬜ | | |
| F2.11 | 🟡 | AppListScreenTest › f2_11_backClosesList, MainActivityTest › f2_11_backFromListReturnsHome (swipe down in Phase 2) | |
| F2.2 | 🟡 | AppsRepositoryTest › f2_2_*, AppListViewModelTest › f2_2_exposesSortedApps, AppListScreenTest › f2_2_showsAppsAsTextList (hidden apps in Phase 2) | |
| F2.9 | 🟡 | AppsRepositoryTest › f2_9_updatesWhenAppsAreInstalledOrRemoved, AndroidLauncherAppsSourceTest › f2_9_reportsPackageChangesUntilUnregistered | New installs appear without restart (Step 1.3) |
| F2.10 | 🟡 | AppsRepositoryTest › f2_10_marksAppsFromOtherProfilesAndKeepsBothCopies (data; tag in Phase 2) | App clones on the phone (spike) |
| F3.1–F3.4 | ⬜ | | |
| F4.1 | 🟡 | SwipeUpTest › f4_1_*, HomeScreenTest › f4_1_*, MainActivityTest (openList) | Swipe up on the phone |
| F4.2–F4.6 | ⬜ | | |
| F5.1–F5.5 | ⬜ | | |
| F6.1–F6.6 | ⬜ | | |
| F7.1–F7.6 | ⬜ | | |
| F8.1–F8.12 | ⬜ | | |
| F9.1–F9.7 | ⬜ | | |
| F10.1–F10.13 | ⬜ | | Compare with the phone's screen-time screen |
| F11.1–F11.3 | ⬜ | | |
| F12.2–F12.4 | ⬜ | | |
| F12.1 | ❌ | Removed (D-008) | |
| F13.1–F13.5 | ⬜ | | Export → reinstall → import |
| F14.1–F14.6 | ⬜ | | |
| F15 | ❌ | Removed (D-001) | |
| F16.1–F16.16 (excl. F16.13) | ⬜ | | |
| F16.13 | ❌ | Removed (D-006) | |
| F17.1 | 🟡 | FileLoggerTest › f17_1_*, AndroidDiagnosticsTest › f17_1_logcatMirrorUsesMatchingPriority, ZeroRulesTest › f17_1_flagsDirectLogging | |
| F17.2 | 🟡 | LogEntryTest › f17_2_*, LogFileStoreTest › f17_2_*, FileLoggerTest › f17_2_* | |
| F17.3 | 🟡 | LogFileStoreTest › f17_3_* | |
| F17.4 | 🟡 | FileLoggerTest › f17_4_backgroundWriterFlushesOnRequest | |
| F17.5 | 🟡 | CrashHandlerTest › f17_5_*, LogFileStoreTest › f17_5_*, FileLoggerTest › f17_5_*, AppStartLoggingTest › f17_5_crashHandlerIsInstalled | Forced crash → report on next start (Step 1.4) |
| F17.6 | 🟡 | ExitReasonLoggerTest › f17_6_*, AndroidDiagnosticsTest › f17_6_mapsAndroidExitReasons | |
| F17.7 | 🟡 | AppStartLoggingTest › f17_7_logsAppStartWithVersionAndDevice (app start only; other events as features land) | |
| F17.8 | 🟡 | LogEntryTest › f17_8_redactedKeepsOnlyLength (helper; usage checked per feature) | |
| F17.9–F17.11 | ⬜ | Step 1.4 | Export logs |
| F17.12 | 🟡 | FileLoggerTest › f17_12_neverThrowsWhenStorageFails, ExitReasonLoggerTest › f17_12_* | |
| F18.1–F18.7 | ⬜ | | Debug crash-on-start → safe mode; switch to the stock launcher |
| NFR-P1, P2, P4, P5 | ⬜ | | Manual (Step 3.2) |
| NFR-P3 | ⬜ | | |
| NFR-P6 | ⬜ | | APK size check |
| NFR-S1–S7 | ⬜ | | |
| Testing Policy #8 (permissions) | ✅ | PermissionAllowlistTest › nfrS_requestedPermissionsMatchAllowlist | |
| NFR-A1–A4 | ⬜ | | |
| NFR-R1–R8 | ⬜ | | |
| NFR-L1–L3, L5, L6 | ⬜ | | |
| NFR-L4, L7, L8 | 🟡 | ZeroRulesTest › nfrL4_*, nfrL8_* (detekt rules); Lint HardcodedText as error | |

When a feature's work starts, split its row into one row per requirement ID (e.g., F2.4) and list the tests.
