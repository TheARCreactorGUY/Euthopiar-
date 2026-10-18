import urllib.request, ssl
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
for s in ['/static/sdbmini.js', '/static/mainmini.js']:
    req = urllib.request.Request(f'https://play.xpass.top{s}', headers={'Referer': 'https://play.xpass.top/', 'User-Agent': 'Mozilla/5.0'})
    content = urllib.request.urlopen(req, context=ctx).read().decode('utf-8', errors='ignore')
    print(f"Downloaded {s}: {len(content)} bytes")
    with open(f"tools/{s.split('/')[-1]}", "w", encoding="utf-8") as f:
        f.write(content)
