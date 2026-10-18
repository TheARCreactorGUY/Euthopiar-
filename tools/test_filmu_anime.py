import urllib.request

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'Referer': 'https://bingr.one/'}

for anime_id, ep in [(30, 1), (21, 1)]:
    url = f'https://embed.filmu.in/anime/{anime_id}/{ep}'
    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=8) as r:
            html = r.read().decode('utf-8')
            print(f"Filmu Anime {anime_id} Ep {ep} -> Status 200, length: {len(html)}")
            # look for script or video or iframe
            import re
            m = re.findall(r'https?://[^\s"\'`]+', html)
            for link in m[:5]:
                print("   ", link)
    except Exception as e:
        print(f"Filmu Anime {anime_id} Ep {ep} -> {e}")
