import urllib.request, ssl, re, json
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
headers = {'Referer': 'https://www.1shows.org/', 'User-Agent': 'Mozilla/5.0'}
req = urllib.request.Request('https://player.vidzee.wtf/embed/movie/634649', headers=headers)
html = urllib.request.urlopen(req, context=ctx).read().decode('utf-8')

for m in re.finditer(r'/assets/[a-zA-Z0-9_\-]+\.js', html):
    print('Asset:', m.group(0))

for s in re.findall(r'<script[^>]*>(.*?)</script>', html, re.DOTALL):
    for line in s.split('\n'):
        if 'assets/' in line or 'api' in line or 'route' in line:
            print('  Line:', line[:150])
