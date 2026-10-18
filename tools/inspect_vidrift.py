import urllib.request
import re
import json

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Referer': 'https://bingr.one/'
}
req = urllib.request.Request('https://embed.vidrift.in/embed/movie/693134', headers=headers)
with urllib.request.urlopen(req, timeout=10) as r:
    body = r.read().decode('utf-8', errors='ignore')

scripts = re.findall(r'<script[^>]*>(.*?)</script>', body, re.DOTALL)
print('Number of inline scripts:', len(scripts))
for i, s in enumerate(scripts):
    if len(s.strip()) > 0:
        print(f'--- Script {i} (len {len(s)}) ---')
        print(s[:400].strip())
        print('...\n')

srcs = re.findall(r'<script[^>]+src=["\']([^"\']+)["\']', body)
print('External scripts:', srcs)
