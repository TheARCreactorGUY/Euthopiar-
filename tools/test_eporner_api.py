import urllib.request
import ssl

ctx = ssl._create_unverified_context()

mirrors = [
    "https://www.eporner.com",
    "https://www.eporner.net",
    "https://www.eporner.xxx"
]

for m in mirrors:
    url = f"{m}/api/v2/video/search/?query=top&per_page=5&format=json"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=8) as r:
            body = r.read().decode('utf-8')
            print(f"{m} -> HTTP {r.status}, length: {len(body)}, preview: {body[:150]}")
    except Exception as e:
        print(f"{m} -> FAIL: {e}")
