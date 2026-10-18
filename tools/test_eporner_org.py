import urllib.request
import ssl
import json
import re

ctx = ssl._create_unverified_context()

url = "https://www.eporner.org/api/v2/web/search/?query=top&per_page=5&page=1"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
with urllib.request.urlopen(req, context=ctx, timeout=8) as r:
    data = json.loads(r.read().decode('utf-8'))
    print("API SUCCESS: total videos returned:", len(data.get("videos", [])))
    first = data["videos"][0]
    print("First video:", first["title"], "URL:", first["url"])

# Now fetch video page
v_url = first["url"]
req_p = urllib.request.Request(v_url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
with urllib.request.urlopen(req_p, context=ctx, timeout=8) as rp:
    html = rp.read().decode('utf-8')
    dloads = re.findall(r'href="(/dload/[^"]+)"', html)
    print("dload links found:", dloads)
    if dloads:
        d_url = f"https://www.eporner.org{dloads[0]}"
        print("Testing download redirect:", d_url)
        # Test stream probe
        req_d = urllib.request.Request(d_url, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
            'Range': 'bytes=0-1024',
            'Referer': v_url
        })
        with urllib.request.urlopen(req_d, context=ctx, timeout=8) as rd:
            print("STREAM PROBE RESULT: HTTP", rd.status, "Content-Type:", rd.headers.get("Content-Type"), "Final URL:", rd.url[:80])
