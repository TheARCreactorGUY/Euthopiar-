import ssl
import http.client
import re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

conn = http.client.HTTPSConnection('94.75.220.4', 443, context=ctx)
conn.request('GET', '/video-LVCZU3IqAOV/ysc807-boobs-ass/', headers={
    'Host': 'www.eporner.com',
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
})
html = conn.getresponse().read().decode('utf-8', errors='ignore')

# 1. Search for any iframes
iframes = re.findall(r'<iframe[^>]+src=[\'"]([^\'"]+)[\'"]', html)
print("iframes:", iframes)

# 2. Search for any video tags
videos = re.findall(r'<video[^>]*>([\s\S]*?)</video>', html)
print(f"video tags: {len(videos)}")
for v in videos:
    print("  video tag inner:", v[:200])

# 3. Check what else contains /dload/ or download
dl_links = re.findall(r'href=[\'"]([^\'"]*dload[^\'"]*)[\'"]', html)
print("dl_links:", dl_links)

# 4. Check for any json or player data
for m in re.finditer(r'EP\.video\.player\s*=\s*\{([\s\S]*?)\};', html):
    print("EP.video.player:", m.group(1))
