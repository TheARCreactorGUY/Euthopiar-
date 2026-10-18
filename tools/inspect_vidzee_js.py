import urllib.request, ssl, re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

headers = {'Referer': 'https://player.vidzee.wtf/', 'User-Agent': 'Mozilla/5.0'}
req = urllib.request.Request('https://player.vidzee.wtf/assets/index-DA3Lv01s.js', headers=headers)
js = urllib.request.urlopen(req, context=ctx).read().decode('utf-8')

for match in re.findall(r'https?://[^\s"\'<>]+', js):
    if any(k in match for k in ['api', 'core', 'stream', 'embed', 'player', 'vidsrc', 'subs']):
        print('  Found:', match)

for match in re.findall(r'["\'](/[a-zA-Z0-9_\-]+/[^"\']+)["\']', js):
    print('  Found rel:', match)
