# How to view MessageMe

There are **two different things**:

| What | Where you view it | Status |
|---|---|---|
| **Real Android app** | Android Studio + emulator, or a phone | Open `main` and Run `app` |
| **Project display website** | Browser | https://message-me-dusky.vercel.app/ |

---

## 1) View the real Android app (on your machine)

Android Studio must run on a desktop. Open the **`main`** branch (or this remaining-work PR branch), not the old `feature/message-me-android-app` snapshot.

### What you need
- Windows / Mac / Linux
- Android Studio Ladybug+ (or equivalent)
- JDK 17
- Android SDK Platform **36**, Build-Tools **36.x**
- A Pixel emulator with Google APIs (API 36 preferred, API 33 also fine)

### Steps
1. Clone or pull `https://github.com/r3s0lv343vr/MessageMe` (default branch `main`).
2. Android Studio → **File → Open** → the folder that contains `settings.gradle.kts`.
3. Copy `local.properties.example` to `local.properties` and set `sdk.dir` if Android Studio does not write it for you.
4. Wait for **Gradle sync**.
5. **Device Manager** → Pixel + **API 36** → Start.
6. Green **Run** ▶ (configuration: `app`).

You should see the **MessageMe** chat screen.

If the project lives under **OneDrive** (`Documents\AI Cohort Repos\MessageMe`), Run will fail with `:app:hiltJavaCompileDebug` or **Cannot snapshot ...\app\build**. Do **not** use **Build → Clean Project**. **File → Exit**, delete `app\build` in File Explorer, then copy the whole `MessageMe` folder to `C:\Users\<you>\AndroidStudioProjects\MessageMe` and **File → Open** that copy. Building inside OneDrive will keep failing.

Firebase (`app/google-services.json`) is **not** required to open, create, edit, or delete reminders offline.

### First-run smoke
1. **Scheduled** tab: Title `Walk the dog` → Send (time optional; skip the clock for an **overnight letter** at your envelope hour. After that hour today, it becomes **tomorrow**).
2. On first open, **Allow notifications** (recommended) or **Not Now**. Later: Settings → Deliver letters as notifications.
3. Set time a couple of minutes ahead (use the **emulator** clock, top-left) → wait, then open **Received**: the message for that day should be there. Tap it to open only that note; Back returns to that day’s list.
4. Hamburger → calendar: **Week of unopened mail**, unopened / not-finished tiles, tap an unopened letter.
5. Scheduled: **Edit**, **Snooze 10m**, **Delete**.
6. Settings → internal notification toggle, JSON/CSV/PDF export.

### Home-screen unread letter
The round **blue chat icon** on the home screen (often near YouTube) is the **app**, not the widget. The coral dot on it is a notification badge.

Do **not** rely on long-press → Widgets. Pixel lists widgets alphabetically by app name, and an older install will not show MessageMe there at all.

1. Android Studio: tap **Sync Now** if it appears, then the **top green Run** (full install). Do not use Apply Changes.
2. Open MessageMe → **Scheduled** → orange **Add unread letter to Home** → confirm.
3. If the launcher still has no MessageMe under Widgets, **restart the emulator**, Run again, then use the in-app button. You can also search **MessageMe** in the Widgets search box.

The widget is a larger **orange** rounded bubble labeled **You**, with the note text.

If Home already shows **Can't load widget**, this is the Glance build that landed on `main`. Pull the widget-load fix, Run, long-press each grey square → **Remove**, then tap **Add unread letter to Home** again.

---

## 2) Agent / emulator notes

This cloud VM can compile unit tests once an Android SDK is present. It does **not** replace your local Android Studio session for clicking through the UI.

To run unit tests from a clone:

```bash
cp local.properties.example local.properties
# set sdk.dir
./gradlew :app:testDebugUnitTest
```

Instrumentation tests need an emulator or device:

```bash
./gradlew :app:connectedDebugAndroidTest
```

---

## 3) Vercel project display page

**Live URL:** https://message-me-dusky.vercel.app/  
**Vercel dashboard:** https://vercel.com/r3s0lv343vrs-projects/message-me  
**Source:** `/web` (Root Directory `web`, Output Directory `public`)

Prefer Vercel’s GitHub integration for future updates. If a deploy token was ever pasted into chat, revoke it at https://vercel.com/account/tokens and use a dashboard secret instead.

### Redeploy from the dashboard
1. [vercel.com/new](https://vercel.com/new) → import `r3s0lv343vr/MessageMe`.
2. **Root Directory:** `web`
3. **Framework Preset:** Other
4. **Output Directory:** `public`
5. Deploy.

---

## Quick decision guide

- “I want to **use** the reminder app” → Android Studio + emulator (section 1), branch `main`.
- “I want a **link to show people** the project” → https://message-me-dusky.vercel.app/
- Firebase sync / second-device restore → add a real `app/google-services.json` from your Firebase project (do not commit it unless you approve).
