import urllib.request, ssl, re
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
headers = {'Referer': 'https://player.vidzee.wtf/', 'User-Agent': 'Mozilla/5.0'}

req = urllib.request.Request('https://player.vidzee.wtf/assets/Ads-IamQT1xR.js', headers=headers)
js = urllib.request.urlopen(req, context=ctx).read().decode('utf-8')

for m in re.finditer(r'(const|let|var)\s+I\s*=\s*([^;]+);', js):
    print('Found I:', m.group(0))

for m in re.finditer(r'(const|let|var)\s+([a-zA-Z0-9_$]+)\s*=\s*["\'](https?://[^"\']+)["\']', js):
    print('Base URL var:', m.group(0))
