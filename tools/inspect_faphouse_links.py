import urllib.request
import ssl
import re

ctx = ssl._create_unverified_context()
req = urllib.request.Request("https://faphouse.com/", headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
})
with urllib.request.urlopen(req, context=ctx, timeout=10) as r:
    html = r.read().decode('utf-8')
    all_links = set(re.findall(r'href="(/[^"#?]+)"', html))
    print("Sample links on homepage (total %d):" % len(all_links))
    for l in sorted(all_links)[:40]:
        print(" ", l)
