import urllib.request, json
headers = {'User-Agent': 'Mozilla/5.0', 'Referer': 'https://bingr.one/'}

for chunk in [1, 2]:
    req = urllib.request.Request(f'https://api.bingr.one/api/anime/21/episodes?chunk={chunk}', headers=headers)
    with urllib.request.urlopen(req, timeout=5) as r:
        d = json.loads(r.read())
        eps = d.get('episodes', [])
        print(f'Chunk {chunk}: {len(eps)} episodes')
        if eps:
            print(f'   First: Ep {eps[0].get("episode")} - {eps[0].get("title")}')
            print(f'   Last:  Ep {eps[-1].get("episode")} - {eps[-1].get("title")}')
