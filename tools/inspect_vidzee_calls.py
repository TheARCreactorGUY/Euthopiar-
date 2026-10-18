import urllib.request, ssl, re, json
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
headers = {'Referer': 'https://player.vidzee.wtf/', 'User-Agent': 'Mozilla/5.0'}

for name in ['Ads-IamQT1xR.js', 'index-DA3Lv01s.js']:
    url = f'https://player.vidzee.wtf/assets/{name}'
    req = urllib.request.Request(url, headers=headers)
    js = urllib.request.urlopen(req, context=ctx).read().decode('utf-8')
    print(f'=== {name} (len {len(js)}) ===')
    
    # search for fetch, axios, post, get, url, m3u8
    for m in re.finditer(r'(fetch|axios|\.get|\.post)\s*\(([^)]+)\)', js):
        call = m.group(0)
        if len(call) < 150:
            print('  Call:', call)
            
    for m in re.findall(r'["\'](https?://[^"\']+)["\']', js):
        if any(k in m for k in ['vid', 'stream', 'api', 'm3u8', 'source', 'play', 'movie', 'tv']):
            print('  URL:', m)
