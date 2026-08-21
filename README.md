# MessageMe

Android app that lets you message yourself reminders for tasks. Offline-first with Room, exact AlarmManager scheduling, **Scheduled** and **Received** pages, a home-screen unread-letter widget, colour-coded calendar, and optional Firebase Auth + Firestore sync.

**Application ID:** `com.unbound.messageme`  
**Min SDK:** 24 · **Target / Compile SDK:** 36  
**Language:** Kotlin · **UI:** Jetpack Compose · **DI:** Hilt

## How to view this project

- **Real app (Android):** open this repo in Android Studio → start a Pixel emulator → Run `app`.  
  Step-by-step: [`docs/HOW_TO_VIEW.md`](docs/HOW_TO_VIEW.md)
- **Project display website (browser):** https://message-me-dusky.vercel.app/  
  Source: [`web/`](web/) · Vercel project: [message-me](https://vercel.com/r3s0lv343vrs-projects/message-me)  
  Deploy notes: [`docs/HOW_TO_VIEW.md`](docs/HOW_TO_VIEW.md#3-vercel-project-display-page)

---

## Better pathways (recommendations)

These are suggested improvements relative to a shoestring, long-lived V1:

1. **Firebase is fine for V1, not mandatory to ship.** Room-only offline is production-usable today. Enable Firebase when you have a project and `google-services.json`. Prefer **Anonymous Auth first**, then optional email/password upgrade — lowest friction for a self-messaging app.
2. **Longevity on a budget:** stay with Firebase Spark (free tier) until you need multi-region or heavy query load. Alternatives later: Supabase (Postgres + Auth) or a tiny Cloudflare Worker + D1. Do **not** put Admin SDK keys in the Android app.
3. **AI scheduling:** on-device heuristics ship now (no API cost). Add Gemini/OpenAI later behind a settings toggle and user-owned API key if you want generative suggestions.
4. **Exact alarms:** Android 12+ may require the user to allow exact alarms in system settings. The app falls back to inexact alarms when permission is missing; document this in onboarding for reliability-critical users.
5. **Play App Signing:** when you create a Play Console account, enroll in Play App Signing and keep the upload keystore out of git (`keystore.properties` + CI secrets).

---

## Fresh-clone setup

### Requirements

- Android Studio Ladybug+ (or equivalent)
- JDK 17
- Android SDK Platform **36**, Build-Tools **36.x**
- Gradle wrapper included (`./gradlew`)

### Configure SDK

```bash
cp local.properties.example local.properties
# Edit local.properties:
# sdk.dir=/path/to/Android/sdk
```

### Optional Firebase

1. Create a Firebase project.
2. Add an Android app with package `com.unbound.messageme`.
3. Download `google-services.json` into `app/google-services.json` (**do not commit** unless the repo owner approves).
4. Enable **Anonymous Authentication** and **Cloud Firestore**.
5. Rebuild. `BuildConfig.FIREBASE_ENABLED` becomes `true` automatically when the file is present.

Without Firebase the app builds and runs fully offline; Settings → Sync remains disabled.

### Build & run

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

Release-ready bundle (debug signing only until a keystore is supplied):

```bash
./gradlew :app:bundleRelease
```

### Emulators

- Primary: Android 16 (API 36), Pixel + Google APIs  
- Secondary: Android 13 (API 33)

### Tests

```bash
./gradlew :app:testDebugUnitTest
# Instrumentation (emulator/device required):
./gradlew :app:connectedDebugAndroidTest
```

### Permissions notes

- Notification permission is requested **after the first scheduled reminder**, with in-app copy from `strings.xml`.
- Channel ID: `scheduled_messages` · Name: **Scheduled Messages** · Importance: High.
- Exact alarms use `AlarmManager`; pending alarms are restored on `BOOT_COMPLETED`, timezone, and time changes.

---

## Architecture

- **UI:** Jetpack Compose + Navigation + Material 3 (watercolour blue / pastel yellow / orange)
- **State:** MVVM (`MessageMeViewModel`) + Flow/StateFlow
- **Data:** Room (tasks, chat messages, scheduled reminders, sync queue)
- **Scheduling:** AlarmManager for user-facing reminders; WorkManager for deferrable cloud sync
- **Cloud:** `CloudSync` interface → `FirebaseCloudSync` or `NoOpCloudSync`
- **Export:** JSON backup/restore, CSV task export, PDF export
- **AI suggestions:** on-device completion-hour heuristics

### Reminder cadence

- Pre-task: 3h, 1h, 30m, 5m  
- **At the chosen time:** the personal message is delivered on the **Received** page and as a lock-screen notification. Tapping the notification opens only that message; Back shows that day’s received list.  
- **Scheduled** holds notes still waiting to send. After delivery they leave Scheduled and appear under Received for that day.  
- Same-day messages are allowed. If the chosen clock time already passed today, the reminder is sent immediately instead of showing an error. Dates before today are blocked.  
- If no time chosen: **overnight letter** at the user’s **envelope hour** (default 3:00 AM, changeable in Settings). If that hour already passed on the chosen day, it rolls to **the next morning**.  
- Skipping time does **not** add extra 8:00 / 10:00 / 15:00 check-in pings.
- Unacked follow-ups: +30m, +90m, +180m after due → shelve as unacknowledged  
- After ack: completion check +1h, retry +1h, then reschedule / dismiss / complete

### Calendar colours

- Free: blue · Pending: orange · Completed: green · Past due: red · Mixed day: green/orange split

---

## Manual smoke checklist

- [ ] Create reminder with selected time (including later today, and a time that already passed today)  
- [ ] Create reminder without time → overnight letter at envelope hour (next morning if that hour already passed)  
- [ ] Edit, snooze, and delete a reminder from chat  
- [ ] Create reminder a few minutes ahead → it sits on **Scheduled**, then appears on **Received** at that time  
- [ ] Tap the notification → only that message; Back → that day’s Received list  
- [ ] Home screen widget: one bubble from You + unread count; tap opens that letter  
- [ ] Internal notification toggle pauses delivery  
- [ ] System-blocked notifications show Settings CTA  
- [ ] Restart app / reboot → reminders rescheduled  
- [ ] Offline create/edit works  
- [ ] With Firebase configured: sync after reconnect  
- [ ] Calendar colour coding + day task list  
- [ ] Acknowledge / complete / dismiss / reschedule in chat  
- [ ] Export JSON / CSV / PDF and restore JSON  

---

## Known limitations

- Firebase sync requires a real `google-services.json` (not inventable).  
- No production keystore is included; do not deploy to public Play production yet.  
- Instrumentation tests need an emulator/device.  
- AI suggestions are heuristic, not generative.  
- Relative reminder offsets (the “7 message” set) left as the listed 3h/1h/30m/5m + 3 unacked follow-ups for now.

---

## License / ownership

Repository: MessageMe · GitHub handle context: `r3s0lv343vr`
