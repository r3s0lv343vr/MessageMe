# How to view MessageMe

There are **two different things**:

| What | Where you view it | Status |
|---|---|---|
| **Real Android app** | Your computer’s Android emulator, or a phone | You run this locally |
| **Project display website** | Vercel (browser) | Landing page in `/web` — needs your Vercel deploy auth |

---

## 1) View the real Android app (on your machine)

The cloud agent **cannot** open Android Studio or show you an emulator screen. That needs a normal desktop.

### What you need on your computer
- A Windows / Mac / Linux PC
- About 10–20 GB free disk for Android Studio + emulator
- Internet for the first download

### Steps (Android Studio)
1. Download and install [Android Studio](https://developer.android.com/studio).
2. Open Android Studio → **More Actions → SDK Manager** → install:
   - Android SDK Platform **36** (and/or **33**)
   - A **Pixel** system image with Google APIs
3. Get the code:
   - GitHub → `r3s0lv343vr/MessageMe` → branch `feature/message-me-android-app`
   - Code → Download ZIP, **or** clone with git
4. Android Studio → **File → Open** → select the repo folder (the one with `settings.gradle.kts`).
5. Wait for **Gradle sync** (bottom status bar).
6. **Device Manager** (phone icon) → Create Device → **Pixel 6 / Pixel 8** → system image **API 36** → Finish → ▶ Start.
7. Click the green **Run** ▶ button (configuration: `app`).

You should see the **MessageMe** chat screen on the emulator.

Optional: Firebase (`google-services.json`) is **not** required just to open the app.

---

## 2) What would be required for the agent to run the emulator for you?

To run Option A **for you**, the agent would need something this cloud VM does **not** have:

- A graphical desktop + Android Studio UI, **or**
- A cloud device farm account (BrowserStack / Firebase Test Lab / similar) **plus**
  - your login / API token
  - permission to upload the APK and stream the device

Even then you would watch the stream in **that** service’s website — not inside this chat.

**Practical path:** you run Android Studio once locally (section 1). That is the normal way Android apps are viewed during development.

---

## 3) Vercel project display page

The folder `/web` is a simple watercolor landing page that explains the project and links to GitHub / the PR.

### Is Vercel linked to this agent?
**No — not currently.** This environment has:
- no Vercel MCP server
- no `VERCEL_TOKEN` / CLI login

### How to authorize the agent (pick one)

#### Option A — Vercel token (best for the agent to deploy)
1. Open [Vercel Account Tokens](https://vercel.com/account/tokens).
2. Create a token (name it e.g. `cursor-messageme`).
3. In Cursor: send the token to the agent in chat **or** add a secret named `VERCEL_TOKEN` for this cloud agent / environment.
4. Also send (or confirm):
   - your Vercel **team/scope** (username or team slug)
   - preferred project name (e.g. `messageme-display`)
5. Ask: “Deploy the `/web` folder to Vercel.”

The agent can then run something like:
```bash
cd web
npx vercel deploy --prod --token "$VERCEL_TOKEN" --yes
```

#### Option B — You deploy from the Vercel dashboard (no token sharing)
1. Go to [vercel.com/new](https://vercel.com/new).
2. Import the GitHub repo `r3s0lv343vr/MessageMe`.
3. Set:
   - **Root Directory:** `web`
   - **Framework Preset:** Other
   - **Output Directory:** `public`
4. Deploy.
5. Paste the live URL back here so it can be added to the README/PR.

#### Option C — Vercel GitHub integration on the PR branch
Same as Option B; every push to the branch can auto-update the display site.

---

## Quick decision guide

- “I want to **use** the reminder app” → Android Studio + emulator (section 1).
- “I want a **link to show people** the project” → Vercel `/web` (section 3).
- “I want the agent to click Run in Android Studio for me” → not possible in this cloud setup; use section 1 or a paid device-farm account.
