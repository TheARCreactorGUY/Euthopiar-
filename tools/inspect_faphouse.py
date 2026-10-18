import urllib.request
import ssl
import re

ctx = ssl._create_unverified_context()
req = urllib.request.Request("https://faphouse.com/", headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
})
with urllib.request.urlopen(req, context=ctx, timeout=10) as r:
    html = r.read().decode('utf-8')
    print("Faphouse length:", len(html))
    
    # Let's find video links
    all_links = re.findall(r'href="(/videos/[^"#]+)"', html)
    print("All /videos/ links:", all_links[:15])
    
    # In FaphousePlugin.kt:
    # private val thumbBlockSplitter = Pattern.compile("""data-el="Thumb"""")
    # private val urlPattern = Pattern.compile("""href="(/videos/[^"#]+)""")
    # private val idPattern = Pattern.compile("""data-id="(\d+)"""")
    
    cards = re.findall(r'<a[^>]+href="(/videos/\d+/[^"#]+)"', html) or re.findall(r'<a[^>]+href="(/videos/[^"#]+)"[^>]*data-id="(\d+)"', html)
    print("Filtered video cards:", cards[:5])
    
    # Let's inspect the first actual video
    first_video = None
    for link in all_links:
        if re.search(r'\d+', link) and link != '/videos/vr':
            first_video = link
            break
            
    print("Testing first real video:", first_video)
    if first_video:
        v_url = f"https://faphouse.com{first_video}"
        req_v = urllib.request.Request(v_url, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
            'Referer': 'https://faphouse.com/'
        })
        with urllib.request.urlopen(req_v, context=ctx, timeout=10) as rv:
            v_html = rv.read().decode('utf-8')
            print("Video page length:", len(v_html))
            
            # Find video player tags or scripts
            v_tags = re.findall(r'<video[^>]*>.*?</video>', v_html, re.DOTALL)
            print("Video tags found:", len(v_tags), v_tags[:1])
            
            # Find any stream URLs
            media_urls = re.findall(r'(https?://[^\s"\'<>]+\.(?:mp4|m3u8)[^\s"\'<>]*)', v_html)
            print("Media URLs in page:", media_urls[:10])
            
            # Look for JSON configs or __NEXT_DATA__ or player setup
            json_scripts = re.findall(r'<script[^>]*type="application/json"[^>]*>(.*?)</script>', v_html, re.DOTALL)
            print("JSON scripts:", len(json_scripts))
            for js in json_scripts:
                print("JSON script preview:", js[:300])
