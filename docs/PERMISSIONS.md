# Permissions

## Phase 0: declared permissions
None. `<queries>` declares the MAIN/LAUNCHER intent so the launcher can list apps
without `QUERY_ALL_PACKAGES` (which is intentionally NOT used).

## Phase 2-3: Shield
- **AccessibilityService (`ShieldService`)**: no manifest permission to request; the user enables it in
  Android Settings > Accessibility after reading the explanation card in More and tapping
  "Open Accessibility settings". `canRetrieveWindowContent=false`, event type `typeWindowStateChanged` only,
  so it receives the package name of the front window and nothing else (no text, passwords, messages,
  notification content).
- **Usage Access (PACKAGE_USAGE_STATS): evaluated, NOT used.** It has no events: detection needs polling from a
  long-running foreground service (permanent notification, battery cost, latency = polling interval, and the
  service is killed by MIUI). The accessibility service is event-driven (lowest latency) and survives
  process death because the system binds it. Trade-off: Accessibility is a more powerful permission class,
  which is why content access is switched off in its XML and why this is explained before anything is enabled.
- `<queries>` additions (no QUERY_ALL_PACKAGES): handlers of DIAL, SENDTO smsto:, android.settings.SETTINGS,
  SHOW_ALARMS and geo: VIEW, so the never-block list can learn this device's dialer/SMS/settings/clock/maps apps.

## Phase 5: Prayer
| permission | why | grant |
|---|---|---|
| INTERNET | download monthly prayer times from `api.aladhan.com` (HTTPS, 10 s timeouts, no other network use) | install-time |
| POST_NOTIFICATIONS | show adhan reminders (only enforced on Android 13+; the Redmi runs 12) | runtime on 13+, not requested before the user turns reminders on |
| SCHEDULE_EXACT_ALARM | on-time adhan alarms. Android 12 requires the user to allow it; AHMED explains first and opens the system screen only after a tap. Without it an inexact alarm is used and the screen says reminders may be minutes late | special (user) |
| RECEIVE_BOOT_COMPLETED | re-schedule the next adhan after reboot (also listens to TIMEZONE_CHANGED, TIME_SET, MY_PACKAGE_REPLACED, which need no permission) | install-time |
Not used: ACCESS_NETWORK_STATE (a failed request is simply handled as offline), location permission (the location is a fixed constant, never read from the phone), VIBRATE, foreground services.
The receiver is `exported=true` so system broadcasts surely reach it; its only external effect is that another app could trigger a prayer notification/re-schedule. The adhan alarm itself uses an explicit, immutable PendingIntent.

## Planned (added only by the phase that needs it, after an explanation screen + explicit tap)
| permission | phase | why | grant type |
|---|---|---|---|
| ACTIVITY_RECOGNITION | 6 | optional step sensor | runtime |
| ACCESS_NOTIFICATION_POLICY | 7 | Do Not Disturb control | special |

Never used: QUERY_ALL_PACKAGES, NotificationListenerService, reading screen text,
passwords, messages or notification content.

## Phase 7 addition
- `ACCESS_NOTIFICATION_POLICY` (Do Not Disturb access). Needed so the app can appear in the system "Do Not Disturb access" list; the user must still grant it in system settings. Used only to set the interruption filter during a focus session or quiet period and to let the app's own prayer channels bypass DND. Allowed through: calls from starred contacts, alarms. Never reads notifications (no NotificationListenerService); system emergency alerts are never blocked by DND.
