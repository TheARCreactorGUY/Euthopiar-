import urllib.request, ssl, re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
headers = {'Referer': 'https://www.1shows.org/', 'User-Agent': 'Mozilla/5.0'}

req = urllib.request.Request('https://vidsrc-embed.ru/embed/movie/634649', headers=headers)
html = urllib.request.urlopen(req, context=ctx).read().decode('utf-8')

for s in re.findall(r'<iframe[^>]+>', html):
    print('Iframe tag:', s[:200])

for s in re.findall(r'<script[^>]+src=[\'"]([^\'"]+)[\'"]', html):
    print('Script src:', s)

for m in re.finditer(r'var\s+([a-zA-Z0-9_$]+)\s*=\s*([^;]+);', html):
    print('Var:', m.group(1), '=', m.group(2)[:100])
