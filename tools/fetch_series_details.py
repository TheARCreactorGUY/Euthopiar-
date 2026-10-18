import urllib.request, re

url = "https://4khdhub.one/lanterns-series-7739/"
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36'
}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=12) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

with open('tools/series_details_sample.html', 'w', encoding='utf-8') as f:
    f.write(html)

print("Saved tools/series_details_sample.html. Length:", len(html))

# Inspect seasons and episodes
seasons = re.findall(r'<(?:div|section|button)[^>]*id=["\']season[^"\']*["\'][^>]*>(.*?)</(?:div|section|button)>', html, re.DOTALL | re.IGNORECASE)
print(f"Found {len(seasons)} season tags")

ep_links = re.findall(r'<a\s+[^>]*href=["\'](https://greenmotors\.club/\?id=[^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)
print(f"Found {len(ep_links)} greenmotors links:")
for h, t in ep_links[:10]:
    clean_t = re.sub(r'<[^>]+>', ' ', t).strip()
    print(f"  [{clean_t}] -> {h[:60]}...")
