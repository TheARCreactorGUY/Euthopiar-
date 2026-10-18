import subprocess
import json
import sys
from pathlib import Path

ROOT_DIR = Path("C:/Euthopiar")
MOVY_JS = ROOT_DIR / "tools" / "movy.js"

if not MOVY_JS.exists():
    print("[-] movy.js not found at", MOVY_JS)
    sys.exit(1)

movy_code = MOVY_JS.read_text(encoding="utf-8")

# 1. Push movy.js to /data/local/tmp
print("[*] Pushing movy.js to /data/local/tmp...")
subprocess.run(["adb", "push", str(MOVY_JS), "/data/local/tmp/movy.js"], check=True)

# 2. Copy to app files/js_plugins/
print("[*] Copying to files/js_plugins/movy.js via run-as...")
subprocess.run(["adb", "shell", "run-as com.euthopiar cp /data/local/tmp/movy.js files/js_plugins/movy.js"], check=True)
subprocess.run(["adb", "shell", "run-as com.euthopiar chmod 600 files/js_plugins/movy.js"], check=True)

# 3. Pull existing scrapers.json
print("[*] Reading existing scrapers.json from device...")
res = subprocess.run(["adb", "shell", "run-as com.euthopiar cat files/js_plugins/scrapers.json"], capture_output=True, text=True)
if res.returncode != 0:
    print("[-] Failed to read scrapers.json:", res.stderr)
    sys.exit(1)

try:
    scrapers = json.loads(res.stdout)
except Exception as e:
    print("[-] Failed to parse scrapers.json:", e)
    scrapers = []

# Remove existing movy if present
scrapers = [s for s in scrapers if s.get("id") != "movy"]

# Add new movy entry
movy_entry = {
    "id": "movy",
    "repositoryUrl": "https://www.movy.sx",
    "name": "Movy",
    "description": "High-speed multi-CDN streaming provider with 4K UHD, 1080p FHD, and 50+ language subtitles",
    "version": "1.0.0",
    "filename": "movy.js",
    "supportedTypes": ["movie", "tv"],
    "enabled": True,
    "hasSettings": False,
    "logo": "https://www.movy.sx/favicon.ico",
    "code": movy_code
}
scrapers.append(movy_entry)

# Write updated scrapers.json locally
temp_json = ROOT_DIR / "build" / "scrapers_updated.json"
temp_json.write_text(json.dumps(scrapers, indent=2), encoding="utf-8")

# Push updated scrapers.json to device
print("[*] Pushing updated scrapers.json to device...")
subprocess.run(["adb", "push", str(temp_json), "/data/local/tmp/scrapers.json"], check=True)
subprocess.run(["adb", "shell", "run-as com.euthopiar cp /data/local/tmp/scrapers.json files/js_plugins/scrapers.json"], check=True)
subprocess.run(["adb", "shell", "run-as com.euthopiar chmod 600 files/js_plugins/scrapers.json"], check=True)

print("[+] Successfully deployed and registered Movy plugin to Euthopiar on device!")
