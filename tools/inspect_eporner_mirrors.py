import urllib.request
import ssl

ctx = ssl._create_unverified_context()

for m in ["www.eporner.org", "www.eporner.me", "www.eporner.cc", "www.eporner.xxx", "www.eporner.club"]:
    url = f"https://{m}/"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=5) as r:
            html = r.read().decode('utf-8')
            print(f"{m} -> HTTP {r.status}, length: {len(html)}, title snippet:", html[:300])
    except Exception as e:
        print(f"{m} -> FAIL: {e}")
