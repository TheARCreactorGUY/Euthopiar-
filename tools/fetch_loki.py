import urllib.request, re

url = "https://4khdhub.one/loki-series-593/"
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36'
}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=12) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

with open('tools/loki_sample.html', 'w', encoding='utf-8') as f:
    f.write(html)

file_titles = re.findall(r'<div class="file-title">(.*?)</div>', html, re.DOTALL)
print(f"Loki file titles ({len(file_titles)}):")
for t in file_titles[:10]:
    print(" ", t.strip()[:80])

# Check for episode links or buttons
ep_buttons = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
ep_links = []
for h, t in ep_buttons:
    clean = re.sub(r'<[^>]+>', ' ', t).strip()
    if re.search(r'(?:Episode\s*\d+|E\d+|\bEP\s*\d+)', clean, re.I):
        ep_links.append((clean, h))

print(f"\nFound {len(ep_links)} episode-specific links:")
for t, h in ep_links[:10]:
    print(f"  [{t}] -> {h[:60]}...")
