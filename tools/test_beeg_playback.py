import urllib.request
import ssl

ctx = ssl._create_unverified_context()
url = "https://store.externulls.com/video/play_url/415391228387383"
req = urllib.request.Request(url, headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Referer': 'https://beeg.com/',
    'Origin': 'https://beeg.com'
})
with urllib.request.urlopen(req, context=ctx, timeout=8) as r:
    path = r.read().decode('utf-8').strip()
    stream_url = f"https://video.beeg.com/{path}"
    print("M3U8 Stream URL:", stream_url)
    
    # Now fetch the m3u8 content
    req_m3u8 = urllib.request.Request(stream_url, headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
        'Referer': 'https://beeg.com/',
        'Origin': 'https://beeg.com'
    })
    with urllib.request.urlopen(req_m3u8, context=ctx, timeout=8) as rm:
        m3u8_text = rm.read().decode('utf-8')
        print("M3U8 Playlist Content:\n", m3u8_text[:600])
        
        # Test first sub-variant
        lines = [line.strip() for line in m3u8_text.splitlines() if line.strip() and not line.startswith('#')]
        print("First variant URI:", lines[0] if lines else None)
        if lines:
            sub_url = lines[0] if lines[0].startswith('http') else f"https://video.beeg.com/{lines[0]}"
            req_sub = urllib.request.Request(sub_url, headers={
                'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
                'Referer': 'https://beeg.com/',
                'Origin': 'https://beeg.com'
            })
            with urllib.request.urlopen(req_sub, context=ctx, timeout=8) as rsub:
                sub_text = rsub.read().decode('utf-8')
                print("Sub-variant M3U8 Content:\n", sub_text[:400])
