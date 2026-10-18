import urllib.request
import re

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

for u in ['https://vidfast.pro/movie/550', 'https://vidlink.pro/movie/550']:
    req = urllib.request.Request(u, headers={'User-Agent': USER_AGENT, 'Referer': 'https://www.1shows.org/'})
    try:
        with urllib.request.urlopen(req) as r:
            html = r.read().decode('utf-8', errors='ignore')
            print(f"=== {u} ===")
            print("Length:", len(html))
            scripts = re.findall(r'<script[^>]*src="([^"]+)"', html)
            print("Scripts:", scripts[:5])
            inline_scripts = re.findall(r'<script[^>]*>(.*?)</script>', html, re.DOTALL)
            print("Inline scripts count:", len(inline_scripts))
            for s in inline_scripts[:3]:
                if len(s.strip()) > 0:
                    print("Sample inline:", s.strip()[:300])
    except Exception as e:
        print(u, 'ERR:', e)
