import urllib.request
import ssl
import re

ctx = ssl._create_unverified_context()
url = "https://mydaddy.cc/video/4a342b9eaf180eafca/"
req = urllib.request.Request(url, headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Referer': 'https://hqporner.com/'
})
with urllib.request.urlopen(req, context=ctx, timeout=10) as r:
    html = r.read().decode('utf-8')
    scripts = re.findall(r'<script[^>]*>(.*?)</script>', html, re.DOTALL)
    for i, s in enumerate(scripts):
        if len(s) > 100:
            print(f"--- Script {i} ({len(s)} chars) ---")
            print(s)
