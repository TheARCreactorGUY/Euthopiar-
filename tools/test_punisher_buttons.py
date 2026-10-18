import urllib.request, re

url = "https://4khdhub.one/marvels-the-punisher-series-2288/"
headers = {'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36'}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=12) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

# Print sections around buttons
buttons = list(re.finditer(r'<a[^>]+href=["\'](https://greenmotors\.club/[^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL))
print(f"Found {len(buttons)} greenmotors buttons!")
for b in buttons[:10]:
    start = max(0, b.start() - 250)
    snippet = html[start:b.end()]
    clean = re.sub(r'<[^>]+>', ' ', snippet).strip()
    clean = re.sub(r'\s+', ' ', clean)
    print("--------------------")
    print(clean)
