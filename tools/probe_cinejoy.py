import urllib.request
import ssl
import re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36',
    'Referer': 'https://cinejoy.pk/'
}

url = 'https://a.wing.st/js/pa-v3C29kv27u3L_dNdo9j-m.js'
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, context=ctx) as resp:
    code = resp.read().decode('utf-8', errors='ignore')

print('JS length:', len(code))
urls = set(re.findall(r'https?://[a-zA-Z0-9\.\-_/]+', code))
print('URLs in script:')
for u in sorted(urls):
    if any(k in u for k in ['api', 'wing', 'stream', 'hls', 'vdrk', 'sub', 'media', 'movie', 'tv']):
        print(' *', u)

# Let's search for server arrays or objects:
# e.g. ["Lisbon", "Nebula", ...] or servers: [...]
for m in re.finditer(r'(?:server|source|provider|endpoint)[a-zA-Z0-9_]*\s*[:=]\s*(\[[^\]]+\]|\{[^\}]+\})', code, re.I):
    print('Match:', m.group(0)[:200])

# Search for any string array with uppercase server names
for arr in re.findall(r'\[(?:"[A-Za-z0-9_-]+"(?:,\s*)?)+\]', code):
    if any(s in arr for s in ['Lisbon', 'Nebula', 'Helios', 'Moscow']):
        print('Server array:', arr)
