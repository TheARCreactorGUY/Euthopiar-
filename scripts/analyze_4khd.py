import urllib.request
import ssl
import re
import json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

req = urllib.request.Request(
    'https://greenmotors.cc/homelander/',
    headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Referer': 'https://greenmotors.cc/?id=test',
        'Cookie': 'xla=s4t'
    }
)

resp = urllib.request.urlopen(req, context=ctx, timeout=15)
html = resp.read().decode('utf-8', errors='ignore')

with open('scripts/homelander.html', 'w', encoding='utf-8') as f:
    f.write(html)

print("Saved homelander.html, length:", len(html))

# Find forms and actions
forms = re.findall(r'<form[^>]*>.*?</form>', html, re.DOTALL | re.IGNORECASE)
print(f"Total forms: {len(forms)}")
for f in forms:
    print("Form:", f[:200])

# Find scripts
scripts = re.findall(r'<script[^>]*>(.*?)</script>', html, re.DOTALL | re.IGNORECASE)
print(f"Total inline scripts: {len(scripts)}")
for i, s in enumerate(scripts):
    s_clean = s.strip()
    if len(s_clean) > 0:
        print(f"--- Script {i} ({len(s_clean)} chars) ---")
        lines = [line.strip() for line in s_clean.splitlines() if line.strip()]
        for l in lines[:15]:
            print("  ", l[:120])
