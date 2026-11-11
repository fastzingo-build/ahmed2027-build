# AHMED 2027: Final report (Phase 8)

**Bottom line:** all eight phases are written and the pure-Java logic is tested (108 unit tests pass). The Android code has **never been compiled and never run on a device**; the build environment I worked in has no Android SDK and no network for Gradle. **No APK exists yet.** The first real proof is the GitHub Actions build (steps below). Expect a few small compile errors on that first run; the audit already found and fixed one (a missing import in `PrayerSettingsScreen`).

## 1. Audit

| Check | Result | Evidence |
|---|---|---|
| Compiles | **NOT TESTED** (not compiled) | No Android SDK here. Static substitute: every app import resolves to a real class; no app or Android class is used without an import; every `Class.method(` call on an app class names an existing method; brackets balance in all files; all XML parses. This found 1 real error (fixed). |
| Unit tests | **PASS** | javac + JUnitCore: `OK (108 tests)` (ClockMath, Lang, AppFilter, SocialSessionManager, ShieldPolicy, FriendKey, Prayer, Growth, Phase 7) |
| Manifest | **PASS** (by reading) | HOME+DEFAULT via `.HomeAlias`; `<queries>` present; no `QUERY_ALL_PACKAGES`; permissions: INTERNET, POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED, ACCESS_NOTIFICATION_POLICY, each explained in `docs/PERMISSIONS.md`. Stale TODO comments in the manifest were corrected. |
| Strings | **PASS** | EN and AR have identical keys, array lengths and format arguments (script). Grep finds no hardcoded UI text (only a literal "0" counter reset and a "P1/P2" priority prefix). Exception by design: `vocab_seed` is a non-translatable English word list. |
| Security | **PASS** (grep) | No plaintext Friend Key, token or API key in the repo; `friend_key.txt` is git-ignored. Config is PBKDF2WithHmacSHA256, 310000 iterations, 16-byte salt, 32-byte hash. Network calls exist only in `PrayerRepo` (api.aladhan.com) and the YouTube player (youtube-nocookie.com); other links open in the user's browser or apps through Intents. |
| Real Friend Key opens the shipped hash | **NOT TESTED** | The key was never given to me. Run `python3 tools/check_friend_key.py`. |
| Safety | **PASS** (by reading) / device **NOT TESTED** | `NeverBlock` covers dialer, SMS, Settings, Clock, Maps, emergency, system UI; accessibility config has `canRetrieveWindowContent=false` (no screen-text reading); no overlay permission, no `NotificationListenerService`; no lockout path (uninstall and disabling the service always remain possible). |
| Social session tests | **PASS** | midnight, timezone change, reboot, restart, independent quotas, clock moved back (`SocialSessionManagerTest`, see `docs/TESTS.md`) |
| Prayer times vs an official source | **NOT TESTED / PARTIAL** | Aladhan was unreachable from here. Compared only with one third-party table (differences listed in `docs/TESTS.md`); the offline estimate differs by more than 2 minutes for several prayers, so ±10 min manual offsets exist. Compare with your mosque. |
| Navigation: all areas reachable from the 5 tabs | **PASS** (by reading) | Home: today, Qibla/Adhkar via Prayer. Apps: launcher, classes, Shield. Grow: Prayer, Habits, Calm, Personal development, Learn, Fitness. Work: Work, Creator, Focus & notifications, Analytics. More: Shield, Friend Key, language, text size, backup, about. |
| Workflow YAML and `build-apk.sh` | **PASS** | YAML parses; matches the supplied file byte for byte; `bash -n build-apk.sh` OK. `shellcheck` is not installed here (NOT RUN). |

## 2. Features

**Implemented (code written, logic tested where pure, UI unverified):** launcher and app classes (Normal/Limited/Blocked/Hidden/Favorite), Shield, social sessions, Friend Key, prayer times and adhan reminders, Qibla, adhkar, habits, calm tools, personal development, Learn, fitness, Work, Creator, Focus/DND, Analytics, Growth Score, EN/AR with RTL, text size, JSON backup.

**Partial:** Prayer times from Aladhan (parser untested on a real response; offline estimate otherwise); Learn (seed channel links unverified, related videos cannot be fully suppressed); Fitness (no automatic step counting); Focus timers (in-app, stop if Android kills the app); Backup restores items, habit and prayer logs but never the social sessions.

**Not done:** device or Android 12 testing of anything; stable release signing; automatic step counting; Device Owner-style enforcement (deliberately out of scope); pie charts or custom date ranges in Analytics.

## 3. Limitations that cannot be removed
- **Clock manipulation:** setting the clock back is contained by a high-water mark; setting it forward looks like time passing. Clearing app data resets everything.
- **Uninstall / disabling the service:** a normal app cannot prevent either. This is by design (no lockout).
- **Shield bypasses:** websites in a browser, cloned apps or second profiles, Safe mode, apps opened while the service is off.
- **YouTube:** embedded players may still show related videos at the end.
- **Accessibility:** Android can stop the service after updates or aggressive battery management; re-enable it if the Shield banner says it is off.
- **Xiaomi / MIUI / HyperOS battery killers**, needed for reliable adhan and the Shield:
  1. Settings → Apps → Manage apps → AHMED 2027 → **Autostart: on**.
  2. Same page → **Battery saver → No restrictions**.
  3. Recent apps: long-press the AHMED card → **Lock** (padlock).
  4. Settings → Notifications → AHMED 2027 → allow, and keep prayer channels on.
  5. If exact alarms are offered, allow "Alarms & reminders".
  6. Settings → Accessibility → AHMED Shield → on (re-check after phone updates).

## 4. Signing (so updates install over the old version)
Each GitHub runner creates a new random debug keystore, so every build is signed differently and Android refuses to update over the previous one ("package conflicts"). To keep one key:
1. Once, in Termux (needs `pkg install openjdk-17`): `keytool -genkeypair -keystore debug.keystore -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"`
2. `base64 -w0 debug.keystore | gh secret set DEBUG_KEYSTORE_B64` (run inside the repo; the keystore file is git-ignored, keep a private backup).
3. Add this step to the workflow before "Build debug APK":
```yaml
      - name: Restore stable debug keystore
        env:
          DEBUG_KEYSTORE_B64: ${{ secrets.DEBUG_KEYSTORE_B64 }}
        run: |
          mkdir -p ~/.android
          echo "$DEBUG_KEYSTORE_B64" | base64 -d > ~/.android/debug.keystore
```
I did not change the supplied workflow. Until you do this, uninstall the old app before installing a new build (this erases its data, so export a backup first).

## 5. Manual steps for the owner
1. Run the GitHub build (below) and paste me any compile errors.
2. `python3 tools/check_friend_key.py` with the real Friend Key.
3. Enable the AHMED Shield in Accessibility settings, set AHMED as the home app, grant notification (and optionally Do Not Disturb) access.
4. Compare prayer times with your mosque for a few days; set offsets if needed.
5. Walk through the device test plans in `docs/TESTS.md` on Android 12.
6. Verify the 15 seeded YouTube channel links in Learn ("verify link").
7. Decide `Protection.GATE_ALL_CLASS_CHANGES` (see `docs/LIMITATIONS.md`).

## 6. Termux commands
```
termux-setup-storage
cd ~ && rm -rf ahmed2027 build-apk.sh
unzip ~/storage/downloads/ahmed2027-final.zip
cd ahmed2027 && bash ../build-apk.sh
```
The script pushes to a PRIVATE GitHub repo, waits for the build, downloads the APK to Downloads and opens the share sheet. It only prints "Done" if the build really succeeded.
