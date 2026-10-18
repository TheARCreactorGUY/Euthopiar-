import urllib.request
import ssl
import re

ctx = ssl._create_unverified_context()
req = urllib.request.Request("https://faphouse.com/", headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
})
with urllib.request.urlopen(req, context=ctx, timeout=10) as r:
    html = r.read().decode('utf-8')

thumbBlockSplitter = re.compile(r'data-el="Thumb"')
urlPattern = re.compile(r'href="(/videos/[^"#]+)')
idPattern = re.compile(r'data-id="(\d+)"')

blocks = thumbBlockSplitter.split(html)
print("Total blocks:", len(blocks))

items = []
for i in range(1, len(blocks)):
    block = blocks[i][:2500]
    m_url = urlPattern.search(block)
    if not m_url:
        continue
    rel_url = m_url.group(1)
    if rel_url == '/videos/vr':
        continue
    items.append(rel_url)

print("Parsed video items count:", len(items))
print("Sample parsed URLs:", items[:3])

# Test streaming for the first item
first_url = f"https://faphouse.com{items[0]}"
print("\nFetching video page for:", first_url)
req_v = urllib.request.Request(first_url, headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Referer': 'https://faphouse.com/'
})
with urllib.request.urlopen(req_v, context=ctx, timeout=10) as rv:
    v_html = rv.read().decode('utf-8')
    
    # 2. Direct Video Stream URL
    streamUrl = None
    sourceMatcher = re.search(r'<video[^>]+id="video-trailer"[^>]*>.*?<source[^>]+src="([^"]+)"', v_html, re.DOTALL)
    if sourceMatcher:
        streamUrl = sourceMatcher.group(1).replace('&amp;', '&')
    if not streamUrl:
        fallbackMatcher = re.search(r'data-fallback="([^"]+)"', v_html)
        if fallbackMatcher:
            streamUrl = fallbackMatcher.group(1).replace('&amp;', '&')
    if not streamUrl:
        directCdnMatcher = re.search(r'https?://video-nss\.fhcdngroup\.online/[^\s"\'<>]+', v_html)
        if directCdnMatcher:
            streamUrl = directCdnMatcher.group(0).replace('&amp;', '&')
            
    print("Resolved stream URL:", streamUrl)
    if streamUrl:
        # Probe stream
        req_s = urllib.request.Request(streamUrl, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
            'Referer': 'https://faphouse.com/',
            'Range': 'bytes=0-1024'
        })
        with urllib.request.urlopen(req_s, context=ctx, timeout=10) as rs:
            print("STREAM PROBE SUCCESS: HTTP", rs.status, "Content-Type:", rs.headers.get("Content-Type"), "Length:", rs.headers.get("Content-Length"))
