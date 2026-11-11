# PROJECT_STATUS

| module | phase | status | evidence |
|---|---|---|---|
| Gradle skeleton (AGP 8.5.2, Java 17, SDK 34/31/26) | 0 | written, NOT COMPILED | files only |
| Manifest (LAUNCHER + HOME/DEFAULT alias, `<queries>`, no permissions) | 0 | written, NOT TESTED | static read |
| core (Db, Prefs, Clock, ClockMath, Lang, LocaleManager, Items, Backup) | 0 | ClockMath/Lang TESTED (12 pass); rest NOT COMPILED | JUnit run |
| ui components + BottomNav + shell | 0 | written, NOT COMPILED | - |
| AR/EN strings | 0 | key parity verified | diff |
| docs | 0 | done | docs/ |
| GitHub workflow | 0 | copied unchanged from owner | .github/ |
| Home "Today" (clock, Hijri+Gregorian, next prayer hook, intention, 3 tasks, habit ring, social chips, 2-min start, dock) | 1 | written, NOT COMPILED | - |
| Launcher (app list, search, chips, long-press classify/favorite, drawer) | 1 | written, NOT COMPILED; AppFilter TESTED (6) | JUnit |
| Onboarding (4 cards, language, Home chooser) | 1 | written, NOT TESTED | - |
| Settings (language, text size, backup/restore via SAF) | 1 | written, NOT TESTED | - |
| SocialSessionManager (pure) + SQL store | 2 | logic TESTED (14 pass); store NOT COMPILED | JUnit |
| Shield: ShieldPolicy (pure) | 3 | TESTED (10 pass) | JUnit |
| Shield: ShieldService, BlockedActivity, NeverBlock, More-tab explanation + status | 3 | written, NOT COMPILED, NOT TESTED on Android 12 | - |
| Prayer logic: calc fallback, API parser, next/current, window, qibla, offsets, week summary (pure) | 5 | TESTED (19 + 3 policy tests pass); accuracy vs ONE third-party table only; Aladhan NOT reachable/compared | JUnit, docs/TESTS.md |
| Prayer Android layer: repo/cache (DB v2), alarms, receiver, notifications, screens (Prayer, Settings, Qibla, Adhkar), Home hook, focus window | 5 | written, NOT COMPILED, NOT TESTED on Android 12 | - |
| learn / growth / creator / work / focus / analytics | 6-7 | not started | package-info only |

Phases 0-5 STOP. Next: GitHub Actions build (first real compile), `python3 tools/check_friend_key.py` with the real key, the manual Shield and Prayer checks in docs/TESTS.md (compare times with your mosque), then Phase 6 (Growth, prompt 06).

## Phase 6 (prompt 06): written, NOT COMPILED, NOT TESTED on device
Habits, Calm (2-minute start, urge timer, replacement actions, triggers, recovery, evening/weekly reflection), Personal development (English plan, Leitner vocab, reading, exercises, goals), Learn (curated library + restricted player), Fitness (5 sessions, rest rule, walking log, +10% suggestion), Grow tab with weekly Growth Score. Pure logic: 99 unit tests pass. Next: Phase 7 (Work, Creator, Focus, Analytics), Phase 8 (final audit).

## Phase 7 (prompt 07): written, NOT COMPILED, NOT TESTED on device
Work tab now has Work (projects with tasks/bugs/features/milestones/notes, dev timer, journal, repo links, AI shortcuts, resources), Creator (pipeline, calendar, script editor, hashtag sets, publish checklist, weekly goal), Focus & notifications (sessions, quiet periods, DND with honest fallback) and Analytics (custom charts, export, delete my data). Growth Score now includes Creation and Focus once used. 108 pure unit tests pass. Next: Phase 8 final audit.

## Phase 8: audit done, see FINAL_REPORT.md
Static audit fixed 1 compile error (missing import). Still NOT COMPILED, NOT run on a device, no APK exists. Next: run the GitHub build.
