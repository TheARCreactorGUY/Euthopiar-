import urllib.request
import re

UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
req = urllib.request.Request('https://static-eu-cdn.eporner.com/ajax3.js?cache=20261001', headers={'User-Agent': UA})
with urllib.request.urlopen(req) as r:
    js = r.read().decode('utf-8')

print("js length:", len(js))
init_fn = re.search(r'init:\s*function\s*\([^\)]*\)\s*\{([\s\S]*?)\n\s*\},', js)
if init_fn:
    print("init function:")
    for line in init_fn.group(1).split('\n'):
        if any(k in line for k in ['src', 'source', 'url', 'ajax', 'get', 'post', 'load', 'hls', 'mp4']):
            print("  ", line.strip())

# Look for all ajax calls
for m in re.finditer(r'\$\.(?:ajax|get|getJSON|post)\s*\(\s*[\'"]([^\'"]+)[\'"]', js):
    print("Ajax endpoint:", m.group(1))

# Look for EP.video.player methods
methods = re.findall(r'([a-zA-Z0-9_]+)\s*:\s*function\s*\(', js)
print("Methods in ajax3.js:", [m for m in methods if 'player' in m.lower() or 'video' in m.lower() or 'source' in m.lower() or 'stream' in m.lower()][:20])
