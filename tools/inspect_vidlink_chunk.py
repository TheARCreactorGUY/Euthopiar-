import urllib.request, ssl, re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

headers = {'Referer': 'https://vidlink.pro/', 'User-Agent': 'Mozilla/5.0'}

req = urllib.request.Request('https://vidlink.pro/movie/634649', headers=headers)
html = urllib.request.urlopen(req, context=ctx).read().decode('utf-8', errors='ignore')

scripts = re.findall(r'<script[^>]*src=["\']([^"\']+)["\']', html)
print("Scripts on vidlink:", scripts)

for s in scripts:
    if s.startswith('/'):
        s_url = 'https://vidlink.pro' + s
    else:
        s_url = s
    try:
        req = urllib.request.Request(s_url, headers=headers)
        js = urllib.request.urlopen(req, context=ctx).read().decode('utf-8', errors='ignore')
        for match in re.findall(r'["\'](/api/[^"\']+)["\']', js):
            print(f"  {s} -> API: {match}")
    except Exception as e:
        print(f"  {s} -> Error: {e}")
