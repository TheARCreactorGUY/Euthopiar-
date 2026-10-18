import urllib.request, re

url = "https://gamerxyt.com/hubcloud.php?host=hubcloud&id=1cntbuemmbnfueu&token=MXB5Ry90WGt4SHlkWThjbHdoa0MwQW4rZ1ZPVmhwLzdYWlZ3Mm0wUWJ1cz0="
headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36', 'Referer': 'https://hubcloud.ist/'}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=12) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
print("Gamerxyt links for Loki S1:")
for h, t in links:
    clean = re.sub(r'<[^>]+>', ' ', t).strip()
    if any(k in h for k in ['r2', 'valentine', 'hubcloud', 'pixeldrain', 'snvhost', 'fastdl', '10gbps', 'hbplay']) or 'server' in clean.lower():
        print(f"  [{clean}] -> {h}")
