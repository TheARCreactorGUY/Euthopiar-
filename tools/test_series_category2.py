import urllib.request, re

url = "https://4khdhub.one/category/series/"
headers = {'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36'}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=12) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

links = re.findall(r'<a[^>]+href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
seen = set()
for h, t in links:
    if h in seen: continue
    seen.add(h)
    clean = re.sub(r'<[^>]+>', '', t).strip().encode('ascii', 'ignore').decode('ascii')
    if any(k in h.lower() for k in ['season', 'series', 'episode']) or ('202' in h):
        print(f"[{clean[:60]}] -> {h}")
