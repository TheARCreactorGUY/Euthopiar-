import urllib.request
import ssl
import re

ctx = ssl._create_unverified_context()

mainUrl = "https://www.xvideos2.com"
req = urllib.request.Request(mainUrl, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
with urllib.request.urlopen(req, context=ctx, timeout=8) as r:
    html = r.read().decode('utf-8')
    print("xvideos2.com home status:", r.status, "length:", len(html))
    video_links = re.findall(r'href="(/video\.[^/"]+/[^"]*)"', html) or re.findall(r'href="(/video\d+/[^"]*)"', html)
    print("Video links found:", video_links[:3])
    if video_links:
        v_url = f"{mainUrl}{video_links[0]}"
        print("Testing video page:", v_url)
        req_v = urllib.request.Request(v_url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)', 'Referer': mainUrl})
        with urllib.request.urlopen(req_v, context=ctx, timeout=8) as rv:
            v_html = rv.read().decode('utf-8')
            print("Video page status:", rv.status, "length:", len(v_html))
            
            hls_matches = re.findall(r'html5player\.setVideoHLS\(\'([^\']+)\'\)', v_html)
            high_matches = re.findall(r'html5player\.setVideoUrlHigh\(\'([^\']+)\'\)', v_html)
            low_matches = re.findall(r'html5player\.setVideoUrlLow\(\'([^\']+)\'\)', v_html)
            print("HLS matches:", hls_matches)
            print("High MP4:", high_matches)
            print("Low MP4:", low_matches)
            
            target = (hls_matches or high_matches or low_matches or [None])[0]
            if target:
                print("Testing probe on:", target)
                req_stream = urllib.request.Request(target, headers={
                    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
                    'Referer': v_url,
                    'Range': 'bytes=0-1024'
                })
                with urllib.request.urlopen(req_stream, context=ctx, timeout=8) as rs:
                    print("XVIDEOS2 STREAM PROBE SUCCESS: HTTP", rs.status, "Content-Type:", rs.headers.get("Content-Type"))
