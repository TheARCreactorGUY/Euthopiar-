import urllib.request
import ssl
import re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'en-US,en;q=0.9',
}

req = urllib.request.Request('https://4khdhub.one/?s=Spider-Man', headers=headers)
with urllib.request.urlopen(req, context=ctx, timeout=12) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

print('Length:', len(html))
with open('tools/sample_search.html', 'w', encoding='utf-8') as f:
    f.write(html)

print('Saved tools/sample_search.html')
# Print hrefs
all_hrefs = re.findall(r'href=[\'"]([^\'"]+)[\'"]', html)
print('Total hrefs:', len(all_hrefs))
for h in all_hrefs[:30]:
    print('  href:', h)
