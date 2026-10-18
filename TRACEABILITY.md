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
| F2.9 | ✅ | AppsRepositoryTest › f2_9_updatesWhenAppsAreInstalledOrRemoved, AndroidLauncherAppsSourceTest › f2_9_reportsPackageChangesUntilUnregistered | New installs appear without restart (checked on the phone) |
| F2.10 | ✅ | AppsRepositoryTest › f2_10_*, AndroidLauncherAppsSourceTest › f2_10_*, AppListScreenTest › f2_10_d034_*, SameNamesTest › d034_* | I-/II- names on the phone (home, app list, pickers) |
| F3.1, F3.2 | ✅ | FavoritesTest › f3_1_*, f3_2_*, FavoritesEditorTest › f3_*, FavoritesEditorViewModelTest › f3_1_f3_2_* | Reorder by dragging on the phone |
| F3.3, F3.4 | ✅ | FavoritesTest › f3_3_*, f3_4_*, HomeUiStateTest › f3_3_*, f3_4_*, FavoritesEditorTest › f3_3_* | Uninstall a favorite app |
| F4.1 | ✅ | SwipeTest › f4_1_*, HomeScreenTest › f4_1_*, MainActivityTest (openList) | Swipe up on the phone |
| F4.2, F4.3 | ✅ | SwipeTest › f4_2_f4_3_*, HomeScreenTest › f4_2_f4_3_*, HomeUiStateTest › f4_2_f4_3_*, SettingsViewModelTest › f4_2_f4_3_f4_6_*, SettingsScreensTest › f4_2_f4_3_f4_5_* | Choose a swipe app on the phone |
| F4.4 | ✅ | SwipeTest › f4_4_*, MainActivityTest › f4_4_*, SettingsScreensTest › f4_4_swipeDownIsNotASetting, SettingsStorageTest › f4_4_*, LauncherViewModelTest › f4_4_f4_5_*, AccessibilityServiceTest › f4_4_f4_5_* (shade while Accessibility runs, D-033) | Swipe down opens the shade on the phone with Accessibility on |
| F4.5 | ✅ | HomeScreenTest › f4_5_*, HomeUiStateTest › f4_5_*, SettingsViewModelTest › f4_5_*, SettingsScreensTest › f4_2_f4_3_f4_5_*, step4_2_*, AccessibilityServiceTest › f4_4_f4_5_* | Double-tap locks on the phone |
| F4.6 | ✅ | HomeUiStateTest › f4_2_f4_3_*, SettingsViewModelTest › f4_2_f4_3_f4_6_*, SettingsScreensTest › f4_6_* | |
| Step 4.2 (Accessibility service) | ✅ | AccessibilityServiceTest › step4_2_*, SettingsViewModelTest › step4_2_*, SettingsScreensTest › step4_2_* | Service survives overnight + reboot (Phase 4 check) |
| F5.1 | ✅ | AppCustomizationTest › f5_1_*, AppListViewModelTest › f2_8_* | |
| F5.2 | ✅ | HiddenAppsTest › f5_2_*, SettingsScreensTest › f2_3_f2_5_f2_7_f5_2_f5_3_* | |
| F5.3 | ✅ | HiddenAppsTest › f5_3_* | |
| F5.4, F5.5 | ✅ | AppCustomizationTest › f5_4_*, f5_5_*, AppMenuTest › f5_4_*, f5_5_* | |
| F17.8 (renames) | ✅ | AppCustomizationTest › f17_8_customNamesAreNeverLogged | |
| F6.1, F6.2, F6.5 | ✅ | AppearanceTest › f6_*, AppearanceViewModelTest › f6_*, HomeUiStateTest › f6_2_f6_5_*, ZeroThemeTest › f6_1_* | Wallpaper and status bar on the phone |
| Step 2.11 (settings structure) | ✅ | SettingsScreensTest › step2_11_*, SettingsViewModelTest › f17_7_logsPages | Walk through every Settings page |
| F6.6 | ❌ | Removed (D-035) | |
| D-037 (logo) | ✅ | SettingsScreensTest › d037_*, OnboardingScreenTest › step2_9_introScreenshot, f6_1_introLightScreenshot | Icon in Settings → Apps and Recents |
| F6.8 | 🟡 | WallpaperCatalogTest › f6_8_*, WallpaperFitTest › f6_8_*, PhoneWallpapersTest › f6_8_*, d038_*, AndroidWallpaperSystemTest › f6_8_*, DotArtTest › f6_8_*, PhoneWallpaperScreenTest › f6_8_*, PhoneWallpaperViewModelTest › f6_8_*, AppearanceTest › f6_rowsCallActions | Set home / lock / both on the phone; lock screen with the phone's theme app; PNG vs circles look |
| F6.7 | ✅ | ClockFormatTest › f6_7_*, TimeTicksTest › f6_7_*, HomeUiStateTest › f6_7_*, HomeScreenTest › f6_7_* (one line at XL), AppearanceTest › f6_7_* (preview one line at XL), f6_1_f6_2_f6_4_f6_5_changesAreSaved | Seconds tick smoothly on the phone |
| F6.3 | ✅ | ZeroThemeTest › f6_3_everyTextStyleUsesDoto, ComponentsTest › step2_2_gallery* screenshots | Doto readable on the phone |
| F6.4 | ✅ | ZeroThemeTest › f6_4_*, AppearanceViewModelTest › f6_* | |
| F7.1–F7.6 | ⬜ | Deferred (D-039) | |
| F8.8–F8.12 | ✅ | WellbeingViewModelTest › f8_*, WellbeingScreensTest › f8_*, step4_3_*, HoldToConfirmTest › f8_12_* (settings and friction; stop screens in part 2) | Turn on/off with hold on the phone; passed on the phone 2026-10-18 |
| F8.1, F8.2 | ✅ | StopRulesTest › f8_1_*, LaunchGateTest › f8_1_*, StopViewModelTest › f8_1_*, StopScreensTest › f8_1_*, f8_2_*, step4_3_pauseScreenshot, AppListViewModelTest › f8_8_off…f8_1_* | Pause before a flagged app on the phone; passed on the phone 2026-10-18 |
| F8.3, F8.6 | 🟡 | UsageTimeTest › f8_3_*, f8_6_* (midnight, DST, time zone, clock set back), WellbeingStateRepositoryTest › f8_6_*, WellbeingStatusTest › f8_3_*, StopViewModelTest › f8_3_* | Used time roughly matches the phone's screen time; used time OK on the phone, midnight reset of time left passed 2026-10-18; "5 more minutes" reset still to confirm |
| F8.4 | ✅ | StopRulesTest › f8_4_*, StopViewModelTest › f8_4_*, StopScreensTest › f8_4_*, step4_3_limitScreenshotLight | Limit screen and "5 more minutes" on the phone; passed on the phone 2026-10-18 |
| F8.5 | ✅ | StopRulesTest › f8_5_*, AppListViewModelTest › f8_5_* | "12m left" in the app list; passed on the phone 2026-10-18 |
| F8.13–F8.15 | ✅ | PunBagTest › f8_13_*, WellbeingStateRepositoryTest › f8_13_*, StopScreensTest › f8_14_* (always a pun, D-045), screenshots | Dots animate; still with "Remove animations"; passed on the phone 2026-10-18 |
| F8.7 | ✅ | OutsideRulesTest › f8_7_*, OutsideEnforcerTest › f8_7_*, f8_4_*, WellbeingViewModelTest › f8_11_f8_16_* | Limit screen over the app when opened from a notification or recents; passed on the phone 2026-10-18 |
| F8.16–F8.18 | ✅ | OutsideRulesTest › f8_16_*, f8_17_*, f8_18_*, OutsideWatcherTest › f8_16_*, OutsideEnforcerTest › f8_16_*, f8_17_*, StopScreensTest › f8_16_*, step4_3_breakScreenshot, WellbeingViewModelTest › f8_16_f8_18_*, WellbeingScreensTest › f8_9_* | Break after 15 min on YouTube; button countdown; Back does nothing; passed on the phone 2026-10-18 |
| F9.1–F9.5, F9.7–F9.10 | ✅ | FocusRepositoryTest › f9_*, WellbeingRulesTest › f9_3_*, AppListViewModelTest › f9_3_*, HomeUiStateTest › f9_*, HomeScreenTest › f9_*, FocusPickerViewModelTest › f9_*, FocusScreensTest › f9_*, d052_*, d054_*, FocusSettingsViewModelTest › f9_*, FocusSettingsScreensTest › f9_*, step4_5_*, landscape_* | Focus start/end, icon, filtered apps, DND, end early with reason; passed on the phone 2026-10-18 (0.0.26) |
| F9.6 | ⏸ | Deferred to the end (D-054) | |
| F10.1–F10.13 | 🟡 | UsageSummaryTest › f10_*, f8_6_*, HomeStatsTest › f10_*, step4_6_*, landscape_*, InsightsScreenTest › f10_*, step4_6_*, landscape_*, HomeStatsViewModelTest › f10_*, ScreenTimeSettingsViewModelTest › f10_*, ScreenTimeSettingsScreenTest › f10_*, BackArrowTest › d052_screenTime, FocusNavigationTest › d051_* | Compare with the phone's screen-time screen |
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
| F14.3 | ✅ | OnboardingViewModelTest › f14_3_*, OnboardingScreenTest › f14_3_*, AndroidOnboardingSystemTest › f14_3_* | Suggestions match the the phone's default apps |
| F14.4 | ✅ | By design: no permission is requested during onboarding; dialogs open only from their buttons | |
| F14.5 | ✅ | OnboardingViewModelTest › f14_5_*, OnboardingScreenTest › f14_1_f14_5_*, OnboardingGateTest › f14_5_* | |
| F14.6 | ✅ | OnboardingViewModelTest › f14_2_f14_6_*, OnboardingScreenTest › f14_6_*, AndroidOnboardingSystemTest › f14_6_* | Battery dialog on the phone |
| F14.7 | ✅ | LauncherViewModelTest › f14_7_*, NotHomeAppBannerTest › f14_7_* | Line after battery saving resets the home app; Set as default fixes it; passed on the phone 2026-10-18 |
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
| NFR-P1, P2, P4, P5 | 🟡 | HomeAppsTest › nfrP2_*, HomeAppsCacheTest › nfrP2_*, AppsRepositoryTest › nfrP2_* (favorites shown from cache on cold start; load time logged); DiagnosticsViewModelTest › nfrP4_*, DiagnosticsScreenTest › nfrP4_*, AppStartLoggingTest › nfrP4_*, MemorySamplerTest › nfrP4_* (memory in Diagnostics, Memory tab, background samples, failed checks logged) | Manual (Step 3.2): Home instant ✅; cold start ≤ 0.5 s incl. favorites ✅; battery 2026-10-17 ✅; overnight 2026-10-17 ✅; memory ✅ 2026-10-17 (under the 80 MB target, D-042) |
| NFR-P3 | ✅ | AppSearchTest › nfrP3_searchOver300AppsIsFast | |
| NFR-P6 | ⬜ | | APK size check |
| NFR-S4 (explanations, fallback) | 🟡 | SpecialPermissionsTest › step4_1_*, f17_7_*, PermissionExplanationTest › nfrS4_*, f14_4_*, step4_1_* (helper; used from Step 4.3) | Explanation → Android screen → back on the phone |
| NFR-S1–S3, S5–S7 | ⬜ | | |
| Testing Policy #8 (permissions) | ✅ | PermissionAllowlistTest › nfrS_requestedPermissionsMatchAllowlist | |
| NFR-A1 | 🟡 | ComponentsTest › nfrA1_toggleRowIsAnnouncedAsSwitchWithState | TalkBack pass (Step 3.2) |
| NFR-A2 | 🟡 | ComponentsTest › nfrA2_listItemsAreAtLeast48dpAndClickable | |
| NFR-A3 | 🟡 | ZeroThemeTest › nfrA3_textMeetsWcagAaInBothThemes | Light theme dotted text legibility |
| NFR-A4 | ⬜ | | |
| NFR-A5 | ✅ | landscape_* screenshot tests in every module (home, app list, onboarding, settings, wallpapers, wellbeing, focus, diagnostics, safe mode, permission explanation) | Rotate the phone on each screen |
| NFR-R4 | 🟡 | HomeScreenTest › nfrR4_* (side by side; favorites scroll if needed) | Rotate home on the phone |
| NFR-R1–R8 | ⬜ | | |
| NFR-L1–L3, L5, L6 | ⬜ | | |
| NFR-L4, L7, L8 | 🟡 | ZeroRulesTest › nfrL4_*, nfrL8_* (detekt rules); Lint HardcodedText as error | |

When a feature's work starts, split its row into one row per requirement ID (e.g., F2.4) and list the tests.
