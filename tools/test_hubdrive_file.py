import urllib.request, re

url = "https://hubdrive.pics/file/17546872652"
headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36'}
req = urllib.request.Request(url, headers=headers)
try:
    with urllib.request.urlopen(req, timeout=12) as resp:
        html = resp.read().decode('utf-8', errors='ignore')
except Exception as e:
    print("Error fetching hubdrive:", e)
    exit(0)

print(f"Hubdrive HTML length: {len(html)}")
title = re.search(r'<title>(.*?)</title>', html)
if title: print("Title:", title.group(1))

links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
print(f"Found {len(links)} links:")
for h, t in links:
    clean = re.sub(r'<[^>]+>', ' ', t).strip().encode('ascii', 'ignore').decode('ascii')
    print(f"  [{clean}] -> {h}")

for line in html.split('\n'):
    if any(k in line.lower() for k in ['ep', 'episode', 'download', 'hubcloud', 'pixeldrain']):
        print("  MATCH:", line.strip()[:150].encode('ascii', 'ignore').decode('ascii'))
