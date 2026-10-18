import urllib.request
import ssl
import re
import json

ctx = ssl._create_unverified_context()

req = urllib.request.Request("https://faphouse.com/c/amateur/videos", headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
})
with urllib.request.urlopen(req, context=ctx, timeout=10) as r:
    html = r.read().decode('utf-8')
    v_links = [l.split('#')[0] for l in re.findall(r'href="(/videos/[^"#?]+(?:#[^"]*)?)"', html) if l.split('#')[0] != '/videos/vr']
    print("Real video links:", len(v_links))
    target_link = v_links[0]
    print("Actual video link:", target_link)

v_url = f"https://faphouse.com{target_link}"
req_v = urllib.request.Request(v_url, headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Referer': 'https://faphouse.com/'
})
with urllib.request.urlopen(req_v, context=ctx, timeout=10) as rv:
    v_html = rv.read().decode('utf-8')
    print("Video page length:", len(v_html))
    
    # Check for direct video tag
    v_tags = re.findall(r'<video[^>]*>.*?</video>', v_html, re.DOTALL)
    print("Video tags in HTML:", len(v_tags))
    for vt in v_tags:
        print("Video tag:", vt[:300])
        
    # Check for script configs
    scripts = re.findall(r'<script[^>]*>(.*?)</script>', v_html, re.DOTALL)
    for s in scripts:
        if 'video-nss' in s or 'preview' in s or 'media' in s or 'videoUrl' in s:
            print("Relevant script snippet:", s[:400])
