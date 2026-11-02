---
name: euthopiar-pipeline
description: >-
  Operational workflows for Euthopiar Android app and streaming plugins. Use when building APKs, testing on emulator (Mode A) or real Wi-Fi device (Mode B), staging scrapers, syncing C:\Euthopiar-Plugins, or publishing sanitized releases to GitHub.
---

# Euthopiar Master Pipeline Skill

This skill teaches the agent how to navigate, build, test, and release the Euthopiar Android streaming app and its 10 QuickJS provider scrapers.

---

## 1. Workspace Layout
* **Daily Workshop:** `C:\Euthopiar`
* **Plugin Source of Truth:** `C:\Euthopiar-Plugins` (GitHub: `TheARCreactorGUY/Plugins.git`)
* **Open Source Sanitized Mirror:** `C:\Euthopiar Open Source Release` (GitHub: `TheARCreactorGUY/Euthopiar-.git`)
* **Automation & System Tools:** `C:\Euthopiar-System\Control-Center\core_ops.py`
* **Local Wi-Fi Staging Server:** `C:\Euthopiar-System\Sandbox-Hosting` (Port `8080`, bound to `0.0.0.0`)
* **Cold Storage Archive:** `E:\Device backup\Euthopiar-ColdStorage\`

---

## 2. Secrets & Credential Rules
* All real API keys live in `C:\Euthopiar\local.properties` (GITIGNORED):
  * `SUBDL_API_KEY`, `TRAKT_CLIENT_ID`, `TRAKT_CLIENT_SECRET`, `SIMKL_CLIENT_ID`, `TMDB_API_KEY`, `FREEKZ_KEY_TOKEN`.
* Never hardcode keys into `.kt` or `.js` files. Kotlin code accesses `BuildConfig.*`. QuickJS scrapers access `globalThis.FREEKZ_KEY_TOKEN` via `HostFunctions.kt` bridge.
* Simkl OAuth must use Auth V2:
  * URL: `https://simkl.com/oauth2/authorize` with PKCE `code_challenge_method=S256`.
  * Token exchange: `https://api.simkl.com/oauth2/token` passing `code_verifier`.
  * Redirect URI: `Euthopiar://auth/simkl`.

---

## 3. Testing Workflows

### Mode A: PC Android Emulator
* **Install App onto Running Emulator:**
  ```powershell
  cd C:\Euthopiar
  .\gradlew.bat installDebug
  ```
* **Push Scrapers to Emulator:**
  ```powershell
  python -c "import sys; sys.path.insert(0, r'C:\Euthopiar-System\Control-Center'); import core_ops; core_ops.push_plugins_to_device(print)"
  ```
  *(Then restart/reopen the app on the emulator).*

### Mode B: Real Device via Wi-Fi
* **Ensure Local Server is Running:**
  ```powershell
  python -m http.server 8080 --directory "C:\Euthopiar-System\Sandbox-Hosting" --bind 0.0.0.0
  ```
* **Stage Updated APK to Wi-Fi Server:**
  ```powershell
  Copy-Item 'C:\Euthopiar\app\build\outputs\apk\release\app-release.apk' 'C:\Euthopiar-System\Sandbox-Hosting\Euthopiar-v1.0.6-LocalTest.apk' -Force
  ```
* **Stage Updated Plugins to Wi-Fi Server:**
  ```powershell
  Copy-Item 'C:\Euthopiar-Plugins\providers\*' 'C:\Euthopiar-System\Sandbox-Hosting\providers\' -Force
  Copy-Item 'C:\Euthopiar-Plugins\manifest.json' 'C:\Euthopiar-System\Sandbox-Hosting\manifest.json' -Force
  ```
* **On Phone:**
  * To install APK: browse to `http://<LAN-IP>:8080/` in Chrome.
  * To refresh plugins: tap the spinning **Refresh** icon in **Settings > Plugins**.

---

## 4. Release & Publishing Workflows

### Publish App Release to GitHub:
Always use `core_ops.py` to ensure zero secrets leak into the public repository:
```powershell
python -c "import sys; sys.path.insert(0, r'C:\Euthopiar-System\Control-Center'); import core_ops; core_ops.push_to_github(safety_unlocked=True, sanitize_before_push=True, log_callback=print)"
```
* Builds production release package:
```powershell
python -c "import sys; sys.path.insert(0, r'C:\Euthopiar-System\Control-Center'); import core_ops; core_ops.build_production_release(print)"
```

### Push Plugin Repository Updates to GitHub:
```powershell
cd C:\Euthopiar-Plugins
git add -A
git commit -m "feat(providers): update scrapers"
git push origin main
```
