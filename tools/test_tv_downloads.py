import urllib.request, json

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'Referer': 'https://bingr.one/'}

for s, e in [(1, 1), (1, 2), (2, 1), (8, 6)]:
    url = f'https://streamrip.fun/api/download/tv/1399?season={s}&episode={e}'
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=8) as r:
            d = json.loads(r.read())
            dls = d.get('downloads', [])
            print(f"Game of Thrones S{s}E{e} -> {len(dls)} download links")
            for item in dls[:2]:
                print(f"   Server: {item.get('server')} | Quality: {item.get('quality')} | Size: {item.get('size')} | Url: {item.get('url')[:40]}...")
    except Exception as ex:
        print(f"Game of Thrones S{s}E{e} -> {ex}")
