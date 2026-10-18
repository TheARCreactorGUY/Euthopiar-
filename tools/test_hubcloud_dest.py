import urllib.request, re

url = "https://hubcloud.ist/drive/1rvfagmbbbuj2z5"
headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36'}
req = urllib.request.Request(url, headers=headers)
try:
    with urllib.request.urlopen(req, timeout=12) as resp:
        html = resp.read().decode('utf-8', errors='ignore')
except Exception as e:
    print("Error fetching hubcloud:", e)
    exit(0)

print(f"Fetched Hubcloud HTML: {len(html)} bytes")
# Find links
links = re.findall(r'<a[^>]+href=["\'](.*?)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
print(f"Found {len(links)} links:")
for h, t in links[:20]:
    clean = re.sub(r'<[^>]+>', '', t).strip()
    print(f"  href: {h} | text: {clean}")

# Find any episode buttons or text
for line in html.split('\n'):
    if any(k in line.lower() for k in ['episode', 'ep', 's01', 'gamerxyt', 'download']):
        print("  MATCH:", line.strip()[:150])
