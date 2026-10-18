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
    print("mydaddy.cc length:", len(html))
    print("Script tags:", re.findall(r'<script[^>]*>(.*?)</script>', html, re.DOTALL)[:5])
    print("All URLs with mp4 or m3u8:", re.findall(r'(https?:[^\s"\'<>]+\.(?:mp4|m3u8)[^\s"\'<>]*)', html))
    print("Full HTML preview:\n", html[:1500])
