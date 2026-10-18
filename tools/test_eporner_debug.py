import urllib.request
import ssl
import traceback

ctx = ssl._create_unverified_context()

for url in [
    "https://www.eporner.com/",
    "https://www.eporner.com/api/v2/web/search/?query=top&per_page=5&page=1",
    "https://api.eporner.com/api/v2/web/search/?query=top&per_page=5&page=1"
]:
    print(f"\nTesting {url}...")
    req = urllib.request.Request(url, headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
    })
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=10) as r:
            body = r.read().decode('utf-8')
            print(f"SUCCESS: HTTP {r.status}, length: {len(body)}")
            print("Preview:", body[:200])
    except Exception as e:
        print(f"FAILED: {e}")
        traceback.print_exc()
