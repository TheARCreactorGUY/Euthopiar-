import urllib.request, ssl, re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

headers = {'Referer': 'https://vidlink.pro/', 'User-Agent': 'Mozilla/5.0'}

req = urllib.request.Request('https://vidlink.pro/movie/634649', headers=headers)
html = urllib.request.urlopen(req, context=ctx).read().decode('utf-8', errors='ignore')
scripts = re.findall(r'<script[^>]*src=["\']([^"\']+)["\']', html)

for s in scripts:
    s_url = ('https://vidlink.pro' + s) if s.startswith('/') else s
    try:
        req = urllib.request.Request(s_url, headers=headers)
        js = urllib.request.urlopen(req, context=ctx).read().decode('utf-8', errors='ignore')
        if '67127:' in js:
            print(f"FOUND 67127 in {s}")
            idx = js.find('67127:')
            print(js[idx:idx+300])
        if '5196:' in js:
            print(f"FOUND 5196 in {s}")
            idx = js.find('5196:')
            print(js[idx:idx+300])
    except Exception as e:
        pass
