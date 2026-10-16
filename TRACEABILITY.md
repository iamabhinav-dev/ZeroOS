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
| F1.6 | ✅ | HomeScreenTest › f1_6_largestTextKeepsFavoritesVisible, f1_6_favoritesScrollOnlyWhenTheyDontFit, f1_6_favoritesThatFitDontScroll | Many favorites / XL text scroll on the phone |
| F1.7 | ✅ | HomeScreenTest › f1_7_centeredScreenshot, SettingsViewModelTest › f1_3_f1_7_*, SettingsScreensTest › f1_3_f1_7_* | |
| F1.8 | ✅ | HomeSourcesTest › f1_8_*, HomeUiStateTest › f1_3_f1_8_*, HomeScreenTest › f1_8_* | |
| F1.9 | ✅ | HomeScreenTest › f1_9_*, MainActivityTest › f1_9_longPressOpensSettingsAndBackReturnsHome | |
| F2.1 | ✅ | MainActivityTest (swipe up opens list) | |
| F2.3 | ✅ | AppListScreenTest › f2_2_f2_3_* | Keyboard opens on the phone |
| F2.4 | ✅ | AppSearchTest › f2_4_*, AppListViewModelTest › f2_4_*, AppListScreenTest › f2_4_* | |
| F2.5 | ✅ | AppListViewModelTest › f2_5_* | |
| F2.6 | ✅ | AppListScreenTest › f2_6_*, AppListViewModelTest › f2_6_* | |
| F2.7 | ✅ | AppListScreenTest › f2_7_* (jump, slide, highlight screenshots) | Current letter grows and brightens on the phone |
| F2.8 | ✅ | AppMenuTest › f2_8_*, AppListViewModelTest › f2_8_*, AppListScreenTest › f2_8_*, HomeScreenTest › f2_8_*, AppsRepositoryTest › f2_8_*, AndroidLauncherAppsSourceTest › f2_8_* | App info and uninstall dialog open on the phone (incl. a clone); no Uninstall on system apps |
| F2.11 | ✅ | AppListScreenTest › f2_11_*, MainActivityTest › f2_11_* | |
| F2.2 | ✅ | AppsRepositoryTest › f2_2_*, AppListViewModelTest › f2_2_exposesSortedApps, f5_1_f5_4_hiddenAppsRemovedAndCustomNamesUsed, AppListScreenTest › f2_2_showsAppsAsTextList | |
| F2.9 | ✅ | AppsRepositoryTest › f2_9_updatesWhenAppsAreInstalledOrRemoved, AndroidLauncherAppsSourceTest › f2_9_reportsPackageChangesUntilUnregistered | New installs appear without restart (checked on the test phone) |
| F2.10 | ✅ | AppsRepositoryTest › f2_10_*, AndroidLauncherAppsSourceTest › f2_10_*, AppListScreenTest › f2_10_d034_*, SameNamesTest › d034_* | I-/II- names on the test phone (home, app list, pickers) |
| F3.1, F3.2 | ✅ | FavoritesTest › f3_1_*, f3_2_*, FavoritesEditorTest › f3_*, FavoritesEditorViewModelTest › f3_1_f3_2_* | Reorder by dragging on the phone |
| F3.3, F3.4 | ✅ | FavoritesTest › f3_3_*, f3_4_*, HomeUiStateTest › f3_3_*, f3_4_*, FavoritesEditorTest › f3_3_* | Uninstall a favorite app |
| F4.1 | ✅ | SwipeTest › f4_1_*, HomeScreenTest › f4_1_*, MainActivityTest (openList) | Swipe up on the phone |
| F4.2, F4.3 | ✅ | SwipeTest › f4_2_f4_3_*, HomeScreenTest › f4_2_f4_3_*, HomeUiStateTest › f4_2_f4_3_*, SettingsViewModelTest › f4_2_f4_3_f4_6_*, SettingsScreensTest › f4_2_f4_3_f4_5_* | Choose a swipe app on the phone |
| F4.4 | 🟡 | SwipeTest › f4_4_*, MainActivityTest › f4_4_*, SettingsScreensTest › f4_4_swipeDownIsNotASetting, SettingsStorageTest › f4_4_* (notification shade in Phase 4, D-033) | |
| F4.5 | ⬜ | (needs the Accessibility service, Phase 4) | |
| F4.6 | ✅ | HomeUiStateTest › f4_2_f4_3_*, SettingsViewModelTest › f4_2_f4_3_f4_6_*, SettingsScreensTest › f4_6_* | |
| F5.1 | ✅ | AppCustomizationTest › f5_1_*, AppListViewModelTest › f2_8_* | |
| F5.2 | ✅ | HiddenAppsTest › f5_2_*, SettingsScreensTest › f2_3_f2_5_f2_7_f5_2_f5_3_* | |
| F5.3 | ✅ | HiddenAppsTest › f5_3_* | |
| F5.4, F5.5 | ✅ | AppCustomizationTest › f5_4_*, f5_5_*, AppMenuTest › f5_4_*, f5_5_* | |
| F17.8 (renames) | ✅ | AppCustomizationTest › f17_8_customNamesAreNeverLogged | |
| F6.1, F6.2, F6.5 | ✅ | AppearanceTest › f6_*, AppearanceViewModelTest › f6_*, HomeUiStateTest › f6_2_f6_5_*, ZeroThemeTest › f6_1_* | Wallpaper and status bar on the phone |
| Step 2.11 (settings structure) | ✅ | SettingsScreensTest › step2_11_*, SettingsViewModelTest › f17_7_logsPages | Walk through every Settings page |
| F6.6 | ❌ | Removed (D-035) | |
| D-037 (logo) | ✅ | SettingsScreensTest › d037_*, OnboardingScreenTest › step2_9_introScreenshot, f6_1_introLightScreenshot | Icon in Settings → Apps and Recents |
| F6.7 | ✅ | ClockFormatTest › f6_7_*, TimeTicksTest › f6_7_*, HomeUiStateTest › f6_7_*, HomeScreenTest › f6_7_* (one line at XL), AppearanceTest › f6_7_* (preview one line at XL), f6_1_f6_2_f6_4_f6_5_changesAreSaved | Seconds tick smoothly on the phone |
| F6.3 | ✅ | ZeroThemeTest › f6_3_everyTextStyleUsesDoto, ComponentsTest › step2_2_gallery* screenshots | Doto readable on the phone |
| F6.4 | ✅ | ZeroThemeTest › f6_4_*, AppearanceViewModelTest › f6_* | |
| F7.1–F7.6 | ⬜ | | |
| F8.1–F8.12 | ⬜ | | |
| F9.1–F9.7 | ⬜ | | |
| F10.1–F10.13 | ⬜ | | Compare with the phone's screen-time screen |
| F11.1–F11.3 | ⬜ | | |
| F12.2–F12.4 | ⬜ | | |
| F12.1 | ❌ | Removed (D-008) | |
| F13.1 | ✅ | BackupTest › f13_1_*, BackupViewModelTest › f13_1_f13_2_*, BackupScreenTest › f13_rowsAndNeverExported | Export to Downloads on the phone |
| F13.2 | ✅ | BackupTest › f13_2_*, BackupViewModelTest › f13_1_f13_2_*, BackupScreenTest › f13_2_* | Export → reinstall → import |
| F13.3 | ✅ | AutoBackupRulesTest › f13_3_* | Android Auto Backup (system) |
| F13.4 | ✅ | By design: the backup holds the whole LauncherSettings, so new settings are included automatically; BackupTest › f13_1_f13_2_* round-trips every field | |
| F13.5 | ✅ | BackupTest › f13_5_*, f13_2_otherFilesAreNotBackups, BackupScreenTest › f13_5_* | |
| F14.1 | ✅ | OnboardingViewModelTest › f14_1_*, OnboardingScreenTest › f14_1_*, step2_9_* screenshots | Read the intro on the phone |
| F14.2 | ✅ | OnboardingViewModelTest › f14_2_*, OnboardingScreenTest › f14_2_*, AndroidOnboardingSystemTest › f14_2_* | "Set as default" shows Android's home-app dialog |
| F14.3 | ✅ | OnboardingViewModelTest › f14_3_*, OnboardingScreenTest › f14_3_*, AndroidOnboardingSystemTest › f14_3_* | Suggestions match the test phone's default apps |
| F14.4 | ✅ | By design: no permission is requested during onboarding; dialogs open only from their buttons | |
| F14.5 | ✅ | OnboardingViewModelTest › f14_5_*, OnboardingScreenTest › f14_1_f14_5_*, OnboardingGateTest › f14_5_* | |
| F14.6 | ✅ | OnboardingViewModelTest › f14_2_f14_6_*, OnboardingScreenTest › f14_6_*, AndroidOnboardingSystemTest › f14_6_* | Battery dialog on the test phone |
| F18.7 (onboarding) | ✅ | OnboardingScreenTest › f14_2_f18_7_* | |
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
| NFR-P1, P2, P4, P5 | 🟡 | HomeAppsTest › nfrP2_*, HomeAppsCacheTest › nfrP2_*, AppsRepositoryTest › nfrP2_* (favorites shown from cache on cold start; load time logged) | Manual (Step 3.2): Home instant; cold start ≤ 0.5 s incl. favorites |
| NFR-P3 | ✅ | AppSearchTest › nfrP3_searchOver300AppsIsFast | |
| NFR-P6 | ⬜ | | APK size check |
| NFR-S1–S7 | ⬜ | | |
| Testing Policy #8 (permissions) | ✅ | PermissionAllowlistTest › nfrS_requestedPermissionsMatchAllowlist | |
| NFR-A1 | 🟡 | ComponentsTest › nfrA1_toggleRowIsAnnouncedAsSwitchWithState | TalkBack pass (Step 3.2) |
| NFR-A2 | 🟡 | ComponentsTest › nfrA2_listItemsAreAtLeast48dpAndClickable | |
| NFR-A3 | 🟡 | ZeroThemeTest › nfrA3_textMeetsWcagAaInBothThemes | Light theme dotted text legibility |
| NFR-A4 | ⬜ | | |
| NFR-R4 | 🟡 | HomeScreenTest › nfrR4_* (side by side; favorites scroll if needed) | Rotate home on the phone |
| NFR-R1–R8 | ⬜ | | |
| NFR-L1–L3, L5, L6 | ⬜ | | |
| NFR-L4, L7, L8 | 🟡 | ZeroRulesTest › nfrL4_*, nfrL8_* (detekt rules); Lint HardcodedText as error | |

When a feature's work starts, split its row into one row per requirement ID (e.g., F2.4) and list the tests.
