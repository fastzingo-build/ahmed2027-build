# Limitations (honest list)

- Shield (Phase 3) enforces only while the AHMED Shield accessibility service is enabled by the user. Until
  then Blocked/Limited are labels (a banner on the Apps tab says so). HIDDEN only removes the app from AHMED's list.
- NOT TESTED on a device: nothing has been verified on Android 12. In particular unverified: whether Android
  12 lets the service start the blocked screen from the background (a fallback sends the user Home),
  detection latency, service restart after reboot, and behaviour after permission revoke.
- Entry points that are NOT proven covered (do not call an app "blocked" until tested): launcher icon, recents,
  notification tap, deep links / share sheets, split-screen / pop-up windows, Picture-in-picture, an app already
  open before the Shield was enabled (it is caught on the next window change), and apps opened while the
  service is stopped. Known bypasses even when it works: disabling the service in Android Settings, uninstalling
  AHMED, clearing its data, Safe mode, using the website in a browser, a second profile / cloned app
  (e.g. MIUI Second Space / Dual Apps use different package names or users).
- The service ignores keyboards and System UI and only acts on apps that have a launcher icon.
- Missing buttons on the blocked screen, on purpose (no fake buttons): "Enter Friend Key" (Phase 4) and
  "open an alternative activity" (Learn/Prayer/Fitness, Phases 5-6). They will be added with their modules.
- LIMITED is defined here as: a 10-second pause, then "Continue for 10 minutes". The master context did not define it.
- Social sessions: the day is computed in a HOME timezone saved on first use (device zone at that time), so changing
  the phone's timezone cannot start a new day. The cost: if you really move to another timezone, the day boundary
  stays on the old one until data is cleared (a Friend-Key-protected setting will be added in Phase 4).
- Social clock tampering: setting the clock BACK cannot extend a session or reach an earlier unused date
  (a persisted high-water mark of the wall clock is kept; incidents are logged as `tamper_log` items). Setting it
  FORWARD can reach later dates, each with its own single session, and cannot be told apart from time passing.
  Side effect: if the clock is wrongly set far into the future, sessions stay used until real time catches up.
  Clearing app data or reinstalling resets everything and cannot be prevented.
- elapsedRealtime restarts at reboot; after a reboot only the wall clock + high-water mark are used.
- Friend Key (Phase 4):
  - A normal APK cannot stop uninstalling AHMED, clearing its data, or disabling the Shield accessibility
    service in Android Settings. If the friend is unreachable you can always still do that. This is by design:
    there is no lockout. Device Owner / Device Admin would enforce more, but it is NOT enabled and is out of
    scope (it would need a factory-reset-level setup and can lock you out of your own phone).
  - The key is only as secret as the friend and the chat it is sent in. Someone who sees the pasted key (or the
    friend's clipboard) can use it. The screen uses FLAG_SECURE (no screenshots / recents thumbnail), disables
    autofill and suggestions, hides characters by default and wipes the pasted text; Android's clipboard itself
    can still hold the key until it is overwritten.
  - The attempt limiter and unlock timers use the stricter of wall clock and elapsedRealtime plus a wall-clock
    high-water mark. Clearing app data resets the delay and all unlocks. Setting the clock back can make a delay
    appear up to its original length again, never longer.
  - DECISION FOR THE OWNER (`Protection.GATE_ALL_CLASS_CHANGES`): by default only changes that make an app
    LESS restricted (Blocked/Limited -> weaker) need the key; making an app stricter is free. Set the constant to
    `true` to require the key for every class change (the literal wording of prompt 04).
  - Not implemented because no such UI exists yet: "edit social limits" (the 30 min is fixed in code) and
    "change language of rules". Protected now: weakening an app class, switching Shield off, turning the 5-minute
    wait off, restoring a backup (it can overwrite app classes).
  - Friend Key unlock of an app lasts 15 minutes from the correct paste, then the Shield checks again at expiry.
  - Backups contain the unlock/wrong-attempt log (scope, app, time, counts), never the key.
- Phase 1: changing app classes and restoring a backup are NOT yet protected by the Friend Key (Phase 4).
- Phase 1: Habit ring is empty until habits exist (Phase 6).
  Social chips read the quota table; sessions themselves start in Phase 2.
- Hijri date uses Java's Umm al-Qura calendar; Tunisia's official date can differ by one day.
- Pull-up drawer: swipe up on Home (only when scrolled to the end) or tap "All apps". Not tested on device.
- Backup restores items, app_class, habit_log, prayer_log and language/text size. social_sessions is
  exported but never restored, so a restore cannot reset a daily quota.
- Phase 0 is a foundation: nothing is blocked.
- A normal APK cannot prevent uninstall or "clear data". Device Owner would, but
  it is not enabled and is out of scope.
- Wall-clock manipulation cannot be fully prevented (see ARCHITECTURE.md threat model).
- HIDDEN is not BLOCKED: hiding an app only removes it from AHMED's own UI.
- Embedded YouTube cannot fully hide recommendations/Shorts (Phase 6).
- Xiaomi/MIUI may kill background work: autostart + battery "No restrictions" needed (Phase 5+).
- NOT COMPILED here: this build environment has no Android SDK (see TESTS.md).
- No Gradle wrapper is committed (it could not be generated offline). The GitHub
  workflow uses `gradle/actions/setup-gradle` with Gradle 8.7. To use `./gradlew`
  locally run `gradle wrapper --gradle-version 8.7` once.

## Prayer (Phase 5)
- **Location:** 34.574 N, 9.429 E from ONE source (Wikipedia, "Menzel Bouzaiane"). The planning prompt said approx 34.62 / 9.62;
  the verified longitude is ~0.19 deg (about 17 km, ~0.8 min of solar time) further west. A second source (e.g. GeoNames or an
  official survey) was not checked.
- **Aladhan could not be reached from the build environment** (blocked by the network allowlist), so the API response parser
  was written from the documented format and tested on a hand-made sample only. If the real response differs, the parser
  REJECTS the month (nothing is stored) and the app keeps showing the estimate with the "Update" error message.
- **Accuracy evidence is one third-party table** (tunisienumerique.com, Sidi Bouzid CITY, Nov 2026) that does not say which
  authority it follows. It is not the Ministry of Religious Affairs' own page, and it is for the city (about 51 km north of
  Menzel Bouzaiane), not the town. Aladhan method 18 itself was not compared against any source. Treat all times as
  unverified until you compare them with your local mosque; the per-prayer +-10 minute adjustment exists for that.
- **Offline estimate:** raw astronomy (Fajr/Isha 18 deg, standard Asr, sunrise/sunset 0.833 deg) differs from that table by
  Fajr +2..+3, Dhuhr -7, Maghrib -5..-6, Isha -2..-3 minutes (Asr 0). So the estimate adds empirical margins
  (Fajr -3, Dhuhr +7, Maghrib +5, Isha +3) to match it; after that it is within 1 minute of the table on the 7 tested days.
  These margins come from one table; the Sunrise (Chourouk) estimate has no reference at all. Always labelled "estimated".
- Times are cached for the current and next month only; if the phone was never online the estimate is used.
- Reminders are OFF by default. Exact alarms need the user's grant on Android 12; otherwise they can be minutes late. Xiaomi/MIUI
  may still kill alarms/receivers unless Autostart and Battery "No restrictions" are set. NOT TESTED on the device.
- Only the next adhan alarm is scheduled; it re-schedules itself when it fires, on reboot, timezone and clock changes. If the
  phone stays off at the adhan time, that notification is missed (no "late" replay).
- Qibla compass uses the rotation-vector sensor plus the geomagnetic declination, assuming the phone is held flat. Magnetic
  interference (cases, speakers, metal) makes it inaccurate: the numeric bearing (108.2 deg from true north) is the reliable part.
- Adhkar: only short, well-known phrases; no Quran verses and no commentary. Wording/counts should be checked with a trusted book.
- The prayer-focus window (off by default) only pauses LIMITED apps and TikTok/Instagram, for 10 minutes from the adhan, and only
  while the Shield works. It is a pause screen with "Go home" / Friend Key, not a lock; never touches dialer, SMS, Settings, Clock, Maps.
- Hijri date uses Java's Umm al-Qura calendar; Tunisia's official date can differ by one day. No Ramadan-specific behaviour.

## Phase 6 limitations
- Learn: embedded videos may show related videos at the end and this cannot be fully disabled. Seeded official channels open in the YouTube app/browser (outside this app) and every seeded link is marked "verify link" because it could not be checked from the build environment.
- Steps are not counted automatically (no ACTIVITY_RECOGNITION permission); walking minutes are logged by hand.
- Psychology tools are rule-based self-help, not medical advice. Growth Score is a weekly trend, not a grade; Creation and Focus show "—" until Phase 7.
- Vocabulary seed (30 words) and the English plan text are short starter content, not a full curriculum.

## Phase 7 limitations
- Focus timer and dev timer are in-app dialogs: if Android kills the app in the background the timer stops, and DND stays on until the next app start or quiet-period tick. Cancelled sessions log only the minutes actually spent.
- Quiet-period boundaries use inexact alarms and can be a few minutes late. DND controls need the user's explicit grant; without it only the timer works.
- Prayer channels are set to bypass DND only after access is granted and only for channels that already exist.
- Creator: nothing is posted automatically and no accounts are accessed. TikTok/Instagram open only as installed apps (never via browser) so the Shield's session rules always apply.
- "Delete my data" keeps social-session history, the clock-tamper log, app classification and the Friend Key on purpose, and requires the Friend Key. Backup export covers items, habit and prayer logs.
- Analytics shows counts and minutes only; there is no pie chart or custom date range.
