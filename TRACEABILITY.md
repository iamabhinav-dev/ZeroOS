# Requirement → Test Traceability

Every implemented requirement ID must map to at least one automated test, or to a manual phone check when automation isn't possible on the JVM.
Update this file in the same commit as the code and tests.

**Status:** ⬜ not started · 🟡 in progress · ✅ implemented & tested · ❌ removed

| ID | Status | Automated tests (class › test) | Manual check |
|---|---|---|---|
| F1.1 | ✅ | ClockFormatTest › f1_1_*, TimeTicksTest › f1_1_*, HomeScreenTest › f1_1_* | Clock updates each minute; tap opens alarms |
| F1.2 | ✅ | ClockFormatTest › f1_2_*, HomeScreenTest › f1_1_f1_2_* | Tap opens calendar |
| F1.3 | ✅ | HomeSourcesTest › f1_3_*, HomeUiStateTest › f1_3_f1_8_*, HomeScreenTest › f1_1_f1_2_f1_3_* | |
| F1.4 | ✅ | HomeUiStateTest › f1_4_*, HomeScreenTest › f1_4_* | |
| F1.5 | ✅ | By design: home shows no badges or counters (screenshots) | |
| F1.6 | ✅ | HomeScreenTest › f1_6_largestTextKeepsFavoritesVisible | |
| F1.7 | 🟡 | HomeScreenTest › f1_7_centeredScreenshot (setting UI in Step 2.11) | |
| F1.8 | ✅ | HomeSourcesTest › f1_8_*, HomeUiStateTest › f1_3_f1_8_*, HomeScreenTest › f1_8_* | |
| F1.9 | 🟡 | HomeScreenTest › f1_9_*, MainActivityTest › f1_9_* (opens Diagnostics until Settings exists) | |
| F2.1 | ✅ | MainActivityTest (swipe up opens list) | |
| F2.3 | ✅ | AppListScreenTest › f2_2_f2_3_* | Keyboard opens on the phone |
| F2.4 | ✅ | AppSearchTest › f2_4_*, AppListViewModelTest › f2_4_*, AppListScreenTest › f2_4_* | |
| F2.5 | ✅ | AppListViewModelTest › f2_5_* | |
| F2.6 | ✅ | AppListScreenTest › f2_6_*, AppListViewModelTest › f2_6_* | |
| F2.7 | ✅ | AppListScreenTest › f2_7_* | |
| F2.8 | ⬜ | (Step 2.6) | |
| F2.11 | ✅ | AppListScreenTest › f2_11_*, MainActivityTest › f2_11_* | |
| F2.2 | 🟡 | AppsRepositoryTest › f2_2_*, AppListViewModelTest › f2_2_exposesSortedApps, AppListScreenTest › f2_2_showsAppsAsTextList (hidden apps in Phase 2) | |
| F2.9 | 🟡 | AppsRepositoryTest › f2_9_updatesWhenAppsAreInstalledOrRemoved, AndroidLauncherAppsSourceTest › f2_9_reportsPackageChangesUntilUnregistered | New installs appear without restart (Step 1.3) |
| F2.10 | ✅ | AppsRepositoryTest › f2_10_*, AndroidLauncherAppsSourceTest › f2_10_*, AppListScreenTest › f2_10_* | Clone tag on the test phone |
| F3.1, F3.2 | ⬜ | | |
| F3.3, F3.4 | 🟡 | HomeUiStateTest › f3_3_*, f3_4_* (display; editing in Step 2.5) | |
| F4.1 | 🟡 | SwipeUpTest › f4_1_*, HomeScreenTest › f4_1_*, MainActivityTest (openList) | Swipe up on the phone |
| F4.2–F4.6 | ⬜ | | |
| F5.1–F5.5 | ⬜ | | |
| F6.1, F6.2, F6.5, F6.6 | ⬜ | (tokens ready in Step 2.2; settings in Step 2.8) | |
| F6.3 | 🟡 | ComponentsTest › step2_2_gallery* screenshots (Doto everywhere) | Doto readable on the phone |
| F6.4 | 🟡 | ZeroThemeTest › f6_4_textSizeScaleAndMinimum (setting in Step 2.8) | |
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
| F17.9 | 🟡 | DiagnosticsPiecesTest › f17_9_*, DiagnosticsViewModelTest › f17_9_*, DiagnosticsScreenTest › f17_9_*, MainActivityTest › f1_9_* | Export logs on the phone |
| F17.10 | 🟡 | DiagnosticsPiecesTest › f17_10_*, LauncherViewModelTest › f17_10_*, DiagnosticsScreenTest › f17_10_crashBannerButtons | Banner after a forced crash |
| F17.11 | 🟡 | DiagnosticsPiecesTest › f17_11_*, DiagnosticsViewModelTest › f17_11_* | |
| F17.12 | 🟡 | FileLoggerTest › f17_12_neverThrowsWhenStorageFails, ExitReasonLoggerTest › f17_12_* | |
| F18.1, F18.2, F18.6 | 🟡 | CrashLoopGuardTest › f18_*, SafeModeActivityTest › f18_* | Debug crash-on-start → safe mode after 3 starts |
| F18.3 | 🟡 | SafeModeScreenTest › f18_3_*, SafeModeAppsTest › f18_3_* | "Open Home settings" switches to the stock launcher |
| F18.4, F18.5 | 🟡 | By design: SafeModeRoot only gets SafeModeApps (LauncherApps) and crash-report text; reviewed in code | |
| F18.7 | ✅ | README "If ZeroOS breaks" | |
| NFR-P1, P2, P4, P5 | ⬜ | | Manual (Step 3.2) |
| NFR-P3 | ✅ | AppSearchTest › nfrP3_searchOver300AppsIsFast | |
| NFR-P6 | ⬜ | | APK size check |
| NFR-S1–S7 | ⬜ | | |
| Testing Policy #8 (permissions) | ✅ | PermissionAllowlistTest › nfrS_requestedPermissionsMatchAllowlist | |
| NFR-A1 | 🟡 | ComponentsTest › nfrA1_toggleRowIsAnnouncedAsSwitchWithState | TalkBack pass (Step 3.2) |
| NFR-A2 | 🟡 | ComponentsTest › nfrA2_listItemsAreAtLeast48dpAndClickable | |
| NFR-A3 | 🟡 | ZeroThemeTest › nfrA3_textMeetsWcagAaInBothThemes | Light theme dotted text legibility |
| NFR-A4 | ⬜ | | |
| NFR-R1–R8 | ⬜ | | |
| NFR-L1–L3, L5, L6 | ⬜ | | |
| NFR-L4, L7, L8 | 🟡 | ZeroRulesTest › nfrL4_*, nfrL8_* (detekt rules); Lint HardcodedText as error | |

When a feature's work starts, split its row into one row per requirement ID (e.g., F2.4) and list the tests.
