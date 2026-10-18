import urllib.request, re

url = "https://4khdhub.one/category/series/"
headers = {'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36'}
req = urllib.request.Request(url, headers=headers)
try:
    with urllib.request.urlopen(req, timeout=12) as resp:
        html = resp.read().decode('utf-8', errors='ignore')
except Exception as e:
    print("Error fetching series category:", e)
    exit(0)

print(f"Series category HTML length: {len(html)}")
cards = re.findall(r'<a[^>]+href=["\'](https://4khdhub\.[^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
seen = set()
for h, t in cards:
    if h in seen or '/category/' in h or '/page/' in h: continue
    seen.add(h)
    clean = re.sub(r'<[^>]+>', '', t).strip().encode('ascii', 'ignore').decode('ascii')
    if len(clean) > 5:
        print(f"Post: {clean[:80]} -> {h}")
