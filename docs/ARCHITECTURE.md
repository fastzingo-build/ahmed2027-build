# AHMED 2027 — Architecture (Phase 0)

Plain Java + Android SDK. No Kotlin, Room, annotation processors or libraries
(only `junit` for unit tests). Package `com.ahmed2027.app`, minSdk 26,
targetSdk 31, compileSdk 34, AGP 8.5.2, Gradle 8.7, Java 17.

## Modules (packages)
| package | job | phase |
|---|---|---|
| core | Db, Prefs, Clock, ClockMath, Lang, LocaleManager | 0 (done) |
| ui | Card, SectionTitle, PrimaryButton, EmptyState, ErrorState, LoadingState, BottomNav, ShellScreens | 0 (done) |
| launcher | AppLoader, AppClassStore, AppFilter (pure) | 1 (written, not compiled) |
| social, shield | TikTok/Instagram sessions, foreground detection, blocked screen | 2-3 |
| friendkey | PBKDF2 verify, attempt limiter, unlock scopes, cooling-off, FriendKeyActivity | 4 (logic tested, UI not compiled) |
| prayer | times, cache, alarms, log, qibla, adhkar | 5 (logic tested, Android layer not compiled) |
| growth, learn | habits, psychology, personal dev, learn, fitness | 6 |
| work, creator, focus, analytics | | 7 |

Phase 0 only has the empty 5-tab shell. Module packages for later phases contain
a `package-info.java` placeholder.

## UI approach
Views are built in Java (no XML layouts, no fragments, no AppCompat). Colors and
dimensions come from `res/values`. Tab content is produced by
`ShellScreens.build(activity, tab)`; later phases replace the "pending" states.
Language change: `LocaleManager.setLanguage()` then `Activity.recreate()`.
`attachBaseContext` wraps the context with the chosen locale and layout direction.

## Data model (`core/Db`, SQLite v1)
- `items(id, type, title, body, status, due_ts, meta_json, created_ts, updated_ts)`
  generic table for tasks, ideas, notes, vocab, etc. Indexed on (type,status), due_ts.
- `social_sessions(package, local_date, start_wall, start_elapsed, expiry_wall)`
  `UNIQUE(package, local_date)`: the database itself enforces one session per app per day.
- `prayer_log(local_date, prayer, state, logged_wall)` `UNIQUE(local_date, prayer)`
- `app_class(package PK, class, favorite, updated_wall)` class: allowed|limited|blocked|hidden
- `habit_log(habit_id, local_date, done)` `UNIQUE(habit_id, local_date)`
- Migrations: new `if (oldVersion < N)` blocks in `onUpgrade`; never edit old ones.
- `prayer_times` (Phase 5) will be added through a v2 migration.

## Social-session state machine (implemented in Phase 2-3)
```
 ELIGIBLE --explicit Start + 10 s pause--> ACTIVE --now >= expiry--> EXPIRED
     ^                                                                  |
     +------------------- next local date (new row allowed) ------------+
```
- expiry_wall = min(start_wall + 30 min, next local midnight) (`ClockMath.nextLocalMidnight`).
- Leaving/returning the app never changes the state. The row is persisted, so a
  process kill or reboot cannot grant a second session (UNIQUE constraint).
- Remaining time while ACTIVE uses `elapsedRealtime` (`start_elapsed`) cross-checked
  against wall time; the stricter value wins (see threat model).

## Threat model
| threat | what happens | mitigation / honesty |
|---|---|---|
| Clock moved back/forward | wall clock disagrees with elapsedRealtime | `ClockMath.isJump`; keep the stricter state, log it. Cannot be fully prevented: a user can still change time and reboot (elapsed resets), and a new local date legitimately starts a new session. |
| Reboot | elapsedRealtime restarts | reference is invalid (not treated as tampering); persisted `expiry_wall` still applies. |
| Timezone change / DST | local date and midnight shift | expiry uses java.time with the system zone; `local_date` is stored per session; DST days are 23/25 h (tested). Changing zone to a later one can reach the next date earlier: documented limitation. |
| Clear data / uninstall | all state lost | A normal APK cannot prevent this. Stated openly in LIMITATIONS.md. |
| Permission revoked | detection stops | show a status banner, app stays usable, never lock the user out. |
| Process kill | in-memory state lost | all state in SQLite / SharedPreferences. |

## Phase 1 additions
- `ui/HomeScreen` (Today), `ui/AppsScreen` (drawer: search, chips, long-press menu), `ui/MoreScreen` (settings),
  `ui/OnboardingScreen` (4 cards), `ui/Host` (screen -> activity callbacks).
- `prayer/PrayerProvider.next()` is the hook for the Next-prayer card; returns null until Phase 5.
- `core/Items` generic items access (tasks use type `task`, status `open`/`done`).
- `core/Backup` JSON export/import (whitelisted columns, validate-then-replace in one transaction).
- Dock = up to 4 apps with `favorite=1` in app_class.
- Text size: `Prefs.TEXT_SCALES` applied as `Configuration.fontScale` in `LocaleManager.wrap`.

## Phase 2-3 additions
- `social/SocialSessionManager` (pure) + `SqlSessionStore` (Android). `social_sessions.package` holds the quota key
  (`tiktok` | `instagram`); both TikTok package names map to `tiktok`. High-water mark and home zone live in Prefs.
- `shield/ShieldPolicy` (pure decision table), `NeverBlock` (static + device-resolved packages), `ShieldService`
  (AccessibilityService, package name only), `BlockedActivity` (AHMED's own screen: BLOCKED, LIMITED pause,
  SOCIAL start with 10 s pause, SOCIAL used), `LimitedPass`, `ShieldStatus`.
- Flow: window change -> service -> ShieldPolicy.decide -> ALLOW or start BlockedActivity. While a social session is
  ACTIVE the service schedules one re-check at expiry (+0.5 s) for the last known foreground app.
- Hook for Phase 4: BlockedActivity gets a "Friend Key" button when `FriendKey.verify()` exists.

## Phase 4 additions
- Pure (tested): `KeyVerifier`, `FriendKey`, `AttemptLimiter`, `UnlockManager`, `CoolingOff`, `Protection`.
- Android: `FriendKeyStore` (SharedPreferences, `commit()` for limiter/unlocks), `FriendKeyGate` (is it unlocked / ask),
  `FriendKeyActivity` (FLAG_SECURE, paste field, show toggle, status, delay countdown, optional 5-min wait).
- Integration: weakening an app class (AppsScreen), switching Shield off and turning the wait off (More),
  restoring a backup (MainActivity). BlockedActivity has "Ask my friend" -> APP unlock for that package (15 min);
  ShieldPolicy allows it and ShieldService re-checks at expiry.
- Rotating the key: `python3 tools/regen_friend_key.py` (writes friend_key.txt, prints the 4 values for FriendKeyConfig).
- Checking a key: `python3 tools/check_friend_key.py`.

## Phase 5 additions
- Pure (tested): `PrayerCalc` (solar algorithm + Tunisian margins), `AladhanParser`, `DayTimes` (+offsets), `NextPrayer`, `QiblaMath`, `WeekSummary`, `PrayerLocation`.
- Android: `PrayerRepo` (SQLite cache `prayer_times`, DB v2 migration; HttpsURLConnection fetch of this+next month, prune older),
  `PrayerProvider` (Home hook, cache-only, never blocks), `PrayerSettings`, `PrayerAlarms` (one next-adhan alarm, exact when allowed),
  `PrayerReceiver` (adhan + BOOT/TIMEZONE/TIME_SET/PACKAGE_REPLACED -> re-schedule), `PrayerNotifier` (channels per sound), `PrayerBoot` (on app start).
- Data flow: cache -> else estimate (labelled) -> manual offsets applied last. Refresh runs on a single background thread.
- UI: Grow tab (`GrowScreen`) -> `PrayerScreen` (today, countdown, log dialog, 7-day view) -> `PrayerSettingsScreen`, `QiblaScreen`, `AdhkarScreen`.
  `Host.push/pop` gives the shell a small back stack (system Back pops).
- Shield: `ShieldPolicy.PRAYER_PAUSE` when the optional focus window is open; the service wakes once at the earliest of
  session end / unlock end / next adhan.
