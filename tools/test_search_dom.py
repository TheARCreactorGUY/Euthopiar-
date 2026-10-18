import urllib.request, re

url = "https://4khdhub.one/?s=Avengers"
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36'
}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=12) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

with open('tools/search_sample.html', 'w', encoding='utf-8') as f:
    f.write(html)

print("Saved tools/search_sample.html. Length:", len(html))

# Inspect article / post blocks
articles = re.findall(r'<article[^>]*>(.*?)</article>', html, re.DOTALL | re.IGNORECASE)
print(f"Found {len(articles)} <article> tags")
if not articles:
    # Try div cards
    divs = re.findall(r'<div[^>]*class=["\'][^"\']*(?:post|card|movie|item)[^"\']*["\'][^>]*>(.*?)</div>', html, re.DOTALL | re.IGNORECASE)
    print(f"Found {len(divs)} div cards")

# Find all <a> tags with href
links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)
found = []
for href, inner in links:
    if any(k in href for k in ['/category/', '/tag/', '/page/', '#', 'telegram', 'genre']):
        continue
    if re.search(r'4khdhub\.\w+/(?:[a-zA-Z0-9-]+-(?:movie|series|\d+)|[a-zA-Z0-9-]+)/?$', href):
        title = re.sub(r'<[^>]+>', '', inner).strip()
        img_m = re.search(r'<img\s+[^>]*src=["\']([^"\']+)["\']', inner)
        img = img_m.group(1) if img_m else None
        if not title and img_m:
            alt_m = re.search(r'alt=["\']([^"\']+)["\']', inner)
            if alt_m:
                title = alt_m.group(1)
        if title and len(title) > 3 and (title, href) not in found:
            found.append((title, href, img))

print(f"Found {len(found)} candidate links:")
for t, h, img in found[:15]:
    print(f"  Title: {t} -> {h}")
