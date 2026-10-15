# Requirement → Test Traceability

Every implemented requirement ID must map to at least one automated test, or to a manual phone check when automation isn't possible on the JVM.
Update this file in the same commit as the code and tests.

**Status:** ⬜ not started · 🟡 in progress · ✅ implemented & tested · ❌ removed

| ID | Status | Automated tests (class › test) | Manual check |
|---|---|---|---|
| F1.1–F1.9 | ⬜ | | |
| F2.1–F2.11 | ⬜ | | |
| F3.1–F3.4 | ⬜ | | |
| F4.1–F4.6 | ⬜ | | |
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
| F17.1–F17.12 | ⬜ | | Forced crash → report on next start; export logs |
| F18.1–F18.7 | ⬜ | | Debug crash-on-start → safe mode; switch to the stock launcher |
| NFR-P1, P2, P4, P5 | ⬜ | | Manual (Step 3.2) |
| NFR-P3 | ⬜ | | |
| NFR-P6 | ⬜ | | APK size check |
| NFR-S1–S7 | ⬜ | | |
| NFR-A1–A4 | ⬜ | | |
| NFR-R1–R8 | ⬜ | | |
| NFR-L1–L8 | ⬜ | | |

When a feature's work starts, split its row into one row per requirement ID (e.g., F2.4) and list the tests.
