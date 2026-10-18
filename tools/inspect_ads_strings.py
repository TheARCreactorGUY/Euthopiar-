import urllib.request, ssl, re, json
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
headers = {'Referer': 'https://player.vidzee.wtf/', 'User-Agent': 'Mozilla/5.0'}

req = urllib.request.Request('https://player.vidzee.wtf/assets/Ads-IamQT1xR.js', headers=headers)
js = urllib.request.urlopen(req, context=ctx).read().decode('utf-8')

# Find string array in js
match = re.search(r'function\s+(_0x[a-f0-9]+)\s*\(\)\s*\{\s*const\s+(_0x[a-f0-9]+)\s*=\s*(\[[^\]]+\])', js)
if match:
    print('Found string array function:', match.group(1))
    arr_str = match.group(3)
    try:
        arr = json.loads(arr_str.replace("'", '"'))
        print('Array length:', len(arr))
        for item in arr:
            if any(k in item for k in ['http', 'api', 'vidsrc', 'embed', 'player', 'm3u8', 'key', 'stream', 'hls']):
                print('  Interesting item:', item)
    except Exception as e:
        print('JSON load error, raw sample:', arr_str[:300])
else:
    print('No match for string array')

# Let's search all strings in js
strings = re.findall(r'["\']([^"\']{4,80})["\']', js)
print('Total strings:', len(strings))
for s in strings:
    if any(k in s.lower() for k in ['http', 'api', 'vidsrc', 'embed', 'player', 'm3u8', 'stream', 'hls', 'fetch', 'token', 'pass']):
        print('  str:', s)
