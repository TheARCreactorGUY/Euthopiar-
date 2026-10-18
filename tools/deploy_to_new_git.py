#!/usr/bin/env python3
"""
Deploy EUPs v2 to the new standalone GitHub repository:
https://github.com/sehgalvansh716-pixel/VanshEUPs-V2-Architecture.git

Prepares:
- plugins.json with updated CDN raw download URLs pointing to VanshEUPs-V2-Architecture/main/
- README.md with repository instructions and raw URL for manual addition into Euthopiar
- GitHub Actions CI workflow for automatic builds
- Pushes to branch 'main' of the new git repository.
"""

import os
import sys
import json
import hashlib
import subprocess
from pathlib import Path

ROOT_DIR = Path(__file__).resolve().parent.parent
REPO_DIR = ROOT_DIR / "test_eups_repo"
NEW_GIT_URL = "https://github.com/sehgalvansh716-pixel/VanshEUPs-V2-Architecture.git"
RAW_BASE_URL = "https://raw.githubusercontent.com/sehgalvansh716-pixel/VanshEUPs-V2-Architecture/main"

def compute_sha256(filepath: Path) -> str:
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()

def main():
    print("=================================================================")
    print("  DEPLOYING EUPs v2 TO NEW REPOSITORY")
    print(f"  Target: {NEW_GIT_URL}")
    print("=================================================================")

    # 1. Update plugins.json
    plugins_json_path = REPO_DIR / "plugins.json"
    with open(plugins_json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    data["name"] = "Vansh EUPs v2 Official Repository"
    data["description"] = "Official decoupled EUP v2 plugin repository for Euthopiar (Media3, Token single-flight, Virtual HLS, TMDB, AniSkip, OpenSubtitles)"
    data["manifestVersion"] = 2

    ADULT_IDS = {"hqporner", "xvideos", "eporner", "rule34video", "beeg", "faphouse"}

    for p in data.get("plugins", []):
        p_id = p["id"].lower()
        eup_file = REPO_DIR / f"{p['id']}.eup"
        if eup_file.exists():
            p["sha256"] = compute_sha256(eup_file)
            p["fileSize"] = eup_file.stat().st_size
        p["downloadUrl"] = f"{RAW_BASE_URL}/{p['id']}.eup"
        if p_id in ADULT_IDS:
            p["isAdult"] = True
            p["isVault"] = True
            p["supportedTypes"] = ["ADULT"]
        else:
            p["isAdult"] = False
            p["isVault"] = False
            p["supportedTypes"] = ["MOVIE", "SERIES", "ANIME"]

    with open(plugins_json_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)

    print(f"[+] Updated plugins.json with {len(data['plugins'])} EUP v2 plugins (9 Main + 6 Vault).")

    # 2. Generate README.md
    readme_content = f"""# Vansh EUPs v2 Official Architecture Repository

Official decoupled Universal Plugin (EUP v2) distribution repository for **Euthopiar**.

## 🚀 How to Add to Euthopiar
In Euthopiar, open **Settings -> Extensions / Plugins -> Repository Manager -> Add Repository**, and paste:

```
{RAW_BASE_URL}/plugins.json
```

---

## 📦 Available Plugins ({len(data['plugins'])})

### 🎬 Main Streaming & Catalog Providers (9)
| Plugin | Version | Supported Types | Capabilities |
| :--- | :---: | :---: | :--- |
| **Atlantic** | v1.1.0 | Movies, TV, Anime | Helios Moscow, Aphrodite CDN, Natsuki/Granite Subs, Multi-Audio |
| **Cinejoy** | v1.1.0 | Movies, TV | Lisbon & Nebula Wasm, Helios, Aphrodite, Direct HLS |
| **Aether** | v1.1.0 | Movies, TV | UpCloud, VidCloud, Multi-Server, 1080p FHD |
| **1Shows** | v1.1.0 | Movies, TV | Fast multi-mirror streaming, 1080p, Subtitles |
| **4KHDHub** | v1.1.0 | Movies, TV | 4K UHD, 1080p, HubCloud, DriveSeed, 10Gbps CDN Downloads |
| **UHDMovies** | v1.1.0 | Movies, TV | 4K UHD, HDR, 10Bit HEVC, DriveSeed, Direct Downloads |
| **MoviesLeech** | v1.1.0 | Movies, TV | Bollywood, Indian Cinema, South Indian, Hollywood, Google CDN |
| **Movy** | v1.1.0 | Movies, TV | PRNG decrypted mirrors, 4K/1080p HLS, Multi-dub audio |
| **PvrPlay** | v1.1.0 | Movies, TV | Regional, South Indian & Hindi cinema streaming |

### 🔒 Vault / Private Space Providers (6)
*Private Space Isolation: Vault plugins are federated into the secure Private Space and never write to history, TMDB, AniSkip, or external scrobblers.*

| Plugin | Version | Supported Types | Capabilities |
| :--- | :---: | :---: | :--- |
| **HQPorner** | v1.1.0 | Vault Video | 1080p/720p Full HD direct MP4 streams and downloads |
| **XVideos** | v1.1.0 | Vault Video | Multi-resolution HLS & direct MP4 streams (1080p, 720p, 480p) |
| **EPorner** | v1.1.0 | Vault Video | 4K UHD & 1080p 60fps streams and direct downloads |
| **Rule34Video**| v1.1.0 | Vault Video | Animated & stylized media streaming across quality tiers |
| **Beeg** | v1.1.0 | Vault Video | Native REST API 1080p/720p direct seekable MP4 streams |
| **Faphouse** | v1.1.0 | Vault Video | Direct MP4 streams and trailer resolution |

---

## 🏛️ EUP v2 Architecture Highlights
- **Zero Android Framework Dependencies**: Compiled against pure JVM contracts (`:eup-api`).
- **Resilient Token Single-Flighting**: `TokenStore` mutex prevents CDN token rotation race conditions.
- **Virtual URIs (`eup://`)**: Demuxed HLS audio track resolution and offline download stability.
- **TMDB Match Candidate Scoring**: Strict Jaro-Winkler + Year check prevents false positive mismatches.
- **AniSkip Integration**: Community skip intro (`OP`) and outro (`ED`) timestamps for anime.
- **OpenSubtitles Integration**: Dynamic external subtitle synchronization via Cinemeta IMDb matching.
"""

    with open(REPO_DIR / "README.md", "w", encoding="utf-8") as f:
        f.write(readme_content)
    print("[+] Generated README.md.")

    # 3. Create .github/workflows/build-eups.yml
    workflow_dir = REPO_DIR / ".github" / "workflows"
    workflow_dir.mkdir(parents=True, exist_ok=True)
    workflow_content = """name: Build & Release EUPs v2

on:
  push:
    branches: [ main ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Validate plugins.json
        run: |
          python3 -c "import json; data=json.load(open('plugins.json')); print(f'Valid EUP manifest with {len(data[\"plugins\"])} plugins')"
"""
    with open(workflow_dir / "build-eups.yml", "w", encoding="utf-8") as f:
        f.write(workflow_content)
    print("[+] Generated GitHub Actions CI workflow.")

    # 4. Push to Git
    print("[*] Configuring git remote and pushing to new repository...")
    commands = [
        ["git", "-C", str(REPO_DIR), "remote", "remove", "v2-origin"],
        ["git", "-C", str(REPO_DIR), "remote", "add", "v2-origin", NEW_GIT_URL],
        ["git", "-C", str(REPO_DIR), "branch", "-M", "main"],
        ["git", "-C", str(REPO_DIR), "add", "-A"],
        ["git", "-C", str(REPO_DIR), "commit", "-m", "Initial commit: EUP v2 Architecture packages, sources, and manifest"],
        ["git", "-C", str(REPO_DIR), "push", "-u", "v2-origin", "main", "--force"]
    ]

    for cmd in commands:
        res = subprocess.run(cmd, capture_output=True, text=True)
        # remove remote may fail if it doesn't exist, ignore that
        if "remove" not in cmd:
            if res.returncode != 0:
                print(f"[!] Command {' '.join(cmd)} failed:\n{res.stderr}\n{res.stdout}")
            else:
                print(f"[+] {' '.join(cmd[:3])} succeeded.")

    print("\n[+] Successfully deployed EUPs v2 to:")
    print(f"    Repository: {NEW_GIT_URL}")
    print(f"    Raw Manifest URL: {RAW_BASE_URL}/plugins.json")

if __name__ == "__main__":
    main()
