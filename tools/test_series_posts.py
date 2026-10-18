import urllib.request, re

for slug in ["/east-of-eden-series-8250/", "/marvels-the-punisher-series-2288/"]:
    url = f"https://4khdhub.one{slug}"
    headers = {'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36'}
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=12) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
    except Exception as e:
        print(f"Error fetching {slug}: {e}")
        continue

    print(f"\n================ {slug} (length {len(html)}) ================")
    fts = re.findall(r'<div class="file-title">(.*?)</div>', html, re.DOTALL | re.IGNORECASE)
    print(f"Found {len(fts)} .file-title elements:")
    for ft in fts[:5]:
        clean = re.sub(r'<[^>]+>', '', ft).strip().encode('ascii', 'ignore').decode('ascii')
        print("  FT:", clean)

    # Search for links containing Episode or E0
    links = re.findall(r'<a[^>]+href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
    for h, t in links:
        clean = re.sub(r'<[^>]+>', ' ', t).strip().encode('ascii', 'ignore').decode('ascii')
        if any(k in clean.lower() or k in h.lower() for k in ['episode', 'ep ', 'ep0', 'e0', 'e1', 'e2', 'e3', 'e4', 'e5', 'e6', 'e7', 'e8', 'hubcloud', 'greenmotors']):
            print(f"  [{clean[:50]}] -> {h}")
