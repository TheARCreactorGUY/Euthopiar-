import urllib.request
import ssl
import re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

headers = {
    'Referer': 'https://www.viduki.net/',
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
}

req = urllib.request.Request('https://www.viduki.net/_next/static/chunks/16-cd60e7c0459f09eb.js', headers=headers)
code = urllib.request.urlopen(req, context=ctx).read().decode('utf-8')

for term in ['/pepper-key', '/main/servers', 'baseUrl', 'NEXT_PUBLIC', 'api.', 'domain']:
    idx = 0
    while True:
        idx = code.find(term, idx)
        if idx == -1:
            break
        start = max(0, idx - 150)
        end = min(len(code), idx + 250)
        print(f"--- MATCH for '{term}' ---")
        print(code[start:end])
        idx += len(term)
        if idx > len(code) or term in ['baseUrl', 'NEXT_PUBLIC', 'domain'] and idx > 1000:
            break
