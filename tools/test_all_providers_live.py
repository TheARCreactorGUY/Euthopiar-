import urllib.request
import json
import re

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

# 1. Test VidFast API
# VidFast usually has an API like /api/source/{id} or /api/movie/{id} or embedded in HTML
print("--- [1] Checking VidFast ---")
vf_url = "https://vidfast.pro/movie/550"
req = urllib.request.Request(vf_url, headers={"User-Agent": USER_AGENT, "Referer": "https://www.1shows.org/"})
try:
    with urllib.request.urlopen(req) as r:
        vf_html = r.read().decode('utf-8', errors='ignore')
        # Check for api routes in chunks
        vf_chunks = re.findall(r'/_next/static/chunks/([^"]+\.js)', vf_html)
        print("Vidfast chunks:", len(vf_chunks))
        # Look for fetch or api in inline scripts
        for s in re.findall(r'<script[^>]*>(.*?)</script>', vf_html, re.DOTALL):
            if "api" in s or "source" in s or "token" in s:
                print("VidFast inline snippet:", s.strip()[:300])
except Exception as e:
    print("VidFast err:", e)

# 2. Test VidLink API
print("\n--- [2] Checking VidLink ---")
vl_url = "https://vidlink.pro/api/movie/550"
# In vidlink, it's typically https://vidlink.pro/api/b/movie/550 or /api/movie/550
for u in [
    "https://vidlink.pro/api/b/movie/550",
    "https://vidlink.pro/api/movie/550",
    "https://vidlink.pro/api/stream/550"
]:
    req = urllib.request.Request(u, headers={"User-Agent": USER_AGENT, "Referer": "https://vidlink.pro/movie/550", "Origin": "https://vidlink.pro"})
    try:
        with urllib.request.urlopen(req) as r:
            body = r.read().decode('utf-8', errors='ignore')
            print(f"VidLink {u} -> HTTP {r.status}, Body: {body[:200]}")
    except Exception as e:
        print(f"VidLink {u} -> {e}")

# 3. Test Viduki 1, 2, 4
print("\n--- [3] Checking Viduki 1, 2, 4 ---")
for num in ["1", "2", "4"]:
    u = f"https://www.viduki.net/{num}/movie/550"
    req = urllib.request.Request(u, headers={"User-Agent": USER_AGENT, "Referer": "https://www.1shows.org/"})
    try:
        with urllib.request.urlopen(req) as r:
            body = r.read().decode('utf-8', errors='ignore')
            print(f"Viduki /{num} -> HTTP {r.status}, Len: {len(body)}")
            # Search for api calls
            apis = re.findall(r'https?://api\.viduki\.net/[^\s"\'`)]+', body)
            print(f"  Viduki APIs found: {apis}")
    except Exception as e:
        print(f"Viduki /{num} -> {e}")
