import urllib.request, re

url = "https://gamerxyt.com/hubcloud.php?host=hubcloud&id=1rvfagmbbbuj2z5&token=RkZXYy9TY2Q1WWZSdHdycHVobysrSW5VcjhaNHcxaTdPdEFNcWd3SGUybz0="
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36',
    'Referer': 'https://hubcloud.ist/'
}
req = urllib.request.Request(url, headers=headers)
try:
    with urllib.request.urlopen(req, timeout=12) as resp:
        html = resp.read().decode('utf-8', errors='ignore')
except Exception as e:
    print("Error fetching gamerxyt:", e)
    exit(0)

print(f"Fetched gamerxyt HTML: {len(html)} bytes")
links = re.findall(r'<a[^>]+href=["\'](.*?)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
print(f"Found {len(links)} links:")
for h, t in links:
    clean = re.sub(r'<[^>]+>', '', t).strip().encode('ascii', 'ignore').decode('ascii')
    print(f"  href: {h} | text: {clean}")

for line in html.split('\n'):
    if any(k in line.lower() for k in ['episode', 'ep ', 's01', 'file', '.mkv', '.mp4', '.zip', 'pixeldrain']):
        clean_line = line.strip()[:150].encode('ascii', 'ignore').decode('ascii')
        print("  MATCH:", clean_line)
