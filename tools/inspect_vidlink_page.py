import urllib.request, ssl, re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

headers = {'Referer': 'https://vidlink.pro/', 'User-Agent': 'Mozilla/5.0'}

req = urllib.request.Request('https://vidlink.pro/_next/static/chunks/app/movie/%5Bid%5D/page-3041cae3d18df8ad.js', headers=headers)
js = urllib.request.urlopen(req, context=ctx).read().decode('utf-8', errors='ignore')

idx = 0
while True:
    idx = js.find('/api/', idx)
    if idx == -1: break
    print('--- MATCH ---')
    print(js[max(0, idx-100):min(len(js), idx+200)])
    idx += 5
