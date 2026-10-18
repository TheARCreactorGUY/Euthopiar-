import urllib.request
import json

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Referer': 'https://bingr.one/'
}

targets = [
    ("Dune: Part Two", "movie/693134"),
    ("Inception", "movie/27205"),
    ("Avengers: Endgame", "movie/299534"),
    ("Interstellar", "movie/157336"),
    ("Jawan", "movie/872585"),
    ("Breaking Bad S1E1", "tv/1396/1/1"),
    ("Stranger Things S1E1", "tv/66732/1/1"),
    ("Game of Thrones S1E1", "tv/1399/1/1"),
    ("The Boys S1E1", "tv/76479/1/1"),
    ("Attack on Titan S1E1", "tv/1429/1/1")
]

for name, path in targets:
    url = f"https://embed.vidrift.in/embed/{path}"
    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=8) as r:
            body = r.read().decode('utf-8', errors='ignore')
        
        meta_idx = body.find('var embedMeta = ')
        if meta_idx == -1:
            print(f"[{name}] embedMeta not found")
            continue
        start_brace = body.find('{', meta_idx)
        depth = 0
        end_brace = -1
        for i in range(start_brace, len(body)):
            c = body[i]
            if c == '{': depth += 1
            elif c == '}':
                depth -= 1
                if depth == 0:
                    end_brace = i + 1
                    break
        if end_brace == -1:
            print(f"[{name}] matching brace not found")
            continue
        data = json.loads(body[start_brace:end_brace])
        orion = data.get('orionStreams') or []
        evion = data.get('evionUrl')
        print(f"=== {name} ({url}) ===")
        if evion:
            print(f"  Evion Stream: {evion}")
        if orion:
            print(f"  Orion Streams ({len(orion)}):")
            for s in orion:
                raw_url = s.get('url', '')
                final_url = f"https://embed.vidrift.in{raw_url}" if raw_url.startswith('/') else raw_url
                print(f"    - {s.get('name')} [{s.get('type')}] -> {final_url}")
        if not evion and not orion:
            print(f"  No streams found in embedMeta")
    except Exception as e:
        print(f"=== {name} Error: {e} ===")
