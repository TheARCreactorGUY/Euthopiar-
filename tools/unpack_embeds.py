import urllib.request
import ssl
import re
import json

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

embeds = {
    'dexter': ('https://play.xpass.top/e/movie/634649?autostart=true', {'Referer': 'https://www.viduki.net/'}),
    'vidzee': ('https://player.vidzee.wtf/embed/movie/634649?color=0278fd', {'Referer': 'https://www.1shows.org/'}),
    'vidsrc': ('https://vidsrc-embed.ru/embed/movie/634649', {'Referer': 'https://www.1shows.org/'}),
    'vidfast': ('https://vidfast.pro/movie/634649', {'Referer': 'https://www.1shows.org/'}),
    'vidlink': ('https://vidlink.pro/movie/634649', {'Referer': 'https://www.1shows.org/'}),
    'multi_lang': ('https://www.viduki.net/2/movie/634649?color=0278fd', {'Referer': 'https://www.1shows.org/'}),
}

for name, (url, hdrs) in embeds.items():
    print(f"\n=================== UNPACKING {name.upper()} ===================")
    hdrs['User-Agent'] = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    req = urllib.request.Request(url, headers=hdrs)
    try:
        with urllib.request.urlopen(req, context=ctx) as r:
            html = r.read().decode('utf-8', errors='ignore')
            print(f"Status: {r.status}, Length: {len(html)}")
            
            # 1. Search for .m3u8, .mp4, file:, source:, src:
            streams = re.findall(r'(https?://[^\s"\'<>]+?\.(?:m3u8|mp4|mkv)(?:\?[^\s"\'<>]*)?)', html)
            if streams:
                print("  Direct stream URLs found in HTML:", streams)
            
            # 2. Search for iframes
            iframes = re.findall(r'<iframe[^>]*src=["\']([^"\']+)["\']', html)
            if iframes:
                print("  Iframes found:", iframes)
                
            # 3. Search for scripts
            scripts = re.findall(r'<script[^>]*src=["\']([^"\']+)["\']', html)
            if scripts:
                print("  Scripts:", scripts[:5])
                
            # 4. Search for inline JSON / player configs
            sources_match = re.findall(r'sources:\s*(\[[^\]]+\])', html)
            if sources_match:
                print("  sources config:", sources_match[:1])

            # 5. Look for jwplayer, playerjs, artplayer, hls.js
            for pl in ['jwplayer', 'Playerjs', 'Artplayer', 'Hls', 'videojs', 'vidsrc', 'eval(']:
                if pl.lower() in html.lower():
                    print(f"  Detected player keyword: {pl}")
    except Exception as e:
        print(f"Error fetching {name}: {e}")
