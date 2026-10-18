import urllib.request, ssl
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
req = urllib.request.Request('https://play.xpass.top/e/movie/634649?autostart=true', headers={'Referer': 'https://www.viduki.net/', 'User-Agent': 'Mozilla/5.0'})
content = urllib.request.urlopen(req, context=ctx).read().decode('utf-8', errors='ignore')
with open('tools/dexter.html', 'w', encoding='utf-8') as f:
    f.write(content)
print("Saved dexter.html, size:", len(content))
