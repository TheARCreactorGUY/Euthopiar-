import urllib.request, re, json

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'Referer': 'https://bingr.one/'}

for anime_id, ep in [(30, 1), (21, 1)]:
    url = f'https://embed.vidrift.in/embed/anime/{anime_id}/{ep}'
    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=8) as r:
            html = r.read().decode('utf-8')
            m = re.search(r'var\s+embedMeta\s*=\s*(\{.*?\});', html, re.DOTALL)
            if m:
                meta = json.loads(m.group(1))
                print(f"Vidrift Anime {anime_id} Ep {ep} -> OK! Orion: {len(meta.get('orionStreams', []))}, Warm: {len(meta.get('warmStreams', []))}")
            else:
                print(f"Vidrift Anime {anime_id} Ep {ep} -> HTML OK, meta not matched, length {len(html)}")
    except Exception as e:
        print(f"Vidrift Anime {anime_id} Ep {ep} -> {e}")

# Also test streamrip download for anime
for anime_id, ep in [(30, 1), (21, 1)]:
    dl_url = f'https://streamrip.fun/api/download/anime/{anime_id}?episode={ep}'
    try:
        req = urllib.request.Request(dl_url, headers=headers)
        with urllib.request.urlopen(req, timeout=8) as r:
            data = json.loads(r.read().decode('utf-8'))
            print(f"Streamrip Anime {anime_id} Ep {ep} -> {len(data.get('downloads', []))} downloads")
    except Exception as e:
        print(f"Streamrip Anime {anime_id} Ep {ep} -> {e}")
