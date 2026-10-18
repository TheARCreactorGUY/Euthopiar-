import urllib.request
import ssl
import re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'}
chunks = [
    '/_app/immutable/entry/start.Cb-AOP25.js',
    '/_app/immutable/chunks/Dr38vCwT.js',
    '/_app/immutable/chunks/BHL3aMHr.js',
    '/_app/immutable/chunks/BBXW7Mf7.js',
    '/_app/immutable/entry/app.BG_KW6jq.js',
    '/_app/immutable/chunks/rIeG37Rx.js',
    '/_app/immutable/chunks/DsnmJJEf.js',
    '/_app/immutable/chunks/cfFcwhEI.js'
]
for ch in chunks:
    try:
        url = f'https://cinejoy.pk{ch}'
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, context=ctx) as r:
            js = r.read().decode('utf-8', errors='ignore')
        urls = re.findall(r'https?://[a-zA-Z0-9\.\-_/]+', js)
        interesting = [u for u in urls if any(k in u for k in ['api', 'wing', 'stream', 'hls', 'vdrk', 'sub', 'media', 'movie', 'tv'])]
        if interesting:
            print(f'Chunk {ch} interesting URLs:', set(interesting))
        servers = re.findall(r'["\'](Helios|Lisbon|Nebula|Nova|Titan|Moscow|Berlin|Tokyo|Paris|Granite|Wing|Natsuki|Alpha|Omega)["\']', js, re.I)
        if servers:
            print(f'Chunk {ch} servers:', set(servers))
    except Exception as e:
        print(f'Error on {ch}: {e}')
