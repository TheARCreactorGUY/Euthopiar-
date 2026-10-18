import urllib.request, re

url = "https://hubcloud.ist/drive/1cntbuemmbnfueu"
headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36', 'Referer': 'https://greenmotors.club/'}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=12) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
print("HubCloud links:")
for h, t in links:
    clean = re.sub(r'<[^>]+>', ' ', t).strip()
    if 'gamerxyt' in h or 'download' in clean.lower():
        print(f"  [{clean}] -> {h}")
