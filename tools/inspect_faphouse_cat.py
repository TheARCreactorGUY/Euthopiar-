import urllib.request
import ssl
import re

ctx = ssl._create_unverified_context()
url = "https://faphouse.com/c/amateur/videos"
req = urllib.request.Request(url, headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
})
with urllib.request.urlopen(req, context=ctx, timeout=10) as r:
    html = r.read().decode('utf-8')
    print("Category length:", len(html))
    links = re.findall(r'href="([^"]+)"', html)
    video_candidates = [l for l in set(links) if re.search(r'\d+', l)]
    print("Video candidates:", video_candidates[:20])
    
    # Let's search for json data or script tags
    print("Script tags containing video/media:")
    scripts = re.findall(r'<script[^>]*>(.*?)</script>', html, re.DOTALL)
    for s in scripts:
        if 'video' in s or 'preview' in s:
            print("Snippet:", s[:200])
