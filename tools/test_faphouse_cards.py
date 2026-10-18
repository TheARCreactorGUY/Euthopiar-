import urllib.request
import ssl
import re

ctx = ssl._create_unverified_context()
req = urllib.request.Request("https://faphouse.com/", headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
})
with urllib.request.urlopen(req, context=ctx, timeout=10) as r:
    html = r.read().decode('utf-8')
    print("Page length:", len(html))
    thumbs = html.count('data-el="Thumb"')
    print('data-el="Thumb" count on homepage:', thumbs)
    
    # Try category page
    req2 = urllib.request.Request("https://faphouse.com/c/amateur/videos", headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
    })
    with urllib.request.urlopen(req2, context=ctx, timeout=10) as r2:
        cat_html = r2.read().decode('utf-8')
        print('data-el="Thumb" count on category page:', cat_html.count('data-el="Thumb"'))
        
    # Check what classes or attributes cards actually have on homepage
    card_patterns = [
        r'class="[^"]*thumb[^"]*"',
        r'data-el="[^"]+"',
        r'href="/videos/[^"]+"'
    ]
    for cp in card_patterns:
        matches = re.findall(cp, html)
        print(f"Pattern {cp}: {len(matches)} matches (sample: {matches[:3]})")
