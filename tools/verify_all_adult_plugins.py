import json
import urllib.request
import re
import ssl
import http.client

UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'

def doh(hostname):
    try:
        r = urllib.request.Request(f'https://1.1.1.1/dns-query?name={hostname}&type=A', headers={'Accept': 'application/dns-json'})
        with urllib.request.urlopen(r, timeout=4) as res:
            d = json.loads(res.read())
            ips = [x['data'] for x in d.get('Answer', []) if x.get('type') == 1]
            return ips
    except Exception as e:
        return []

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def test_probe(url, headers=None, timeout=6):
    h = {'User-Agent': UA, 'Range': 'bytes=0-1024'}
    if headers: h.update(headers)
    
    # 1. First try standard urllib request
    try:
        req = urllib.request.Request(url, headers=h)
        with urllib.request.urlopen(req, timeout=timeout) as res:
            if res.status in (200, 206):
                ct = res.getheader('Content-Type') or 'video/mp4'
                cl = res.getheader('Content-Length') or 'stream'
                return True, f"HTTP {res.status} ({ct}, {cl})"
    except Exception as e:
        pass

    # 2. Fallback: DoH IP connection with Host header
    try:
        parts = url.split('/')
        host = parts[2]
        path = '/' + '/'.join(parts[3:])
        ips = doh(host)
        for ip in ips[:3]:
            try:
                conn = http.client.HTTPSConnection(ip, 443, context=ctx, timeout=timeout)
                req_headers = {'Host': host}
                req_headers.update(h)
                conn.request('GET', path, headers=req_headers)
                res = conn.getresponse()
                if res.status in (200, 206):
                    ct = res.getheader('Content-Type') or 'video/mp4'
                    cl = res.getheader('Content-Length') or 'stream'
                    return True, f"HTTP {res.status} via DoH ({ct}, {cl})"
            except Exception:
                continue
    except Exception:
        pass

    return False, "Probe failed via standard & DoH"

print("=" * 60)
print("VERIFYING ALL 6 ADULT PLUGINS (STREAM EXTRACTION & PROBES)")
print("=" * 60)

# 1. Rule34Video
print("\n[1/6] RULE34VIDEO")
try:
    req = urllib.request.Request("https://rule34video.com/latest-updates/", headers={'User-Agent': UA})
    with urllib.request.urlopen(req, timeout=5) as r:
        html = r.read().decode('utf-8', errors='ignore')
    v_match = re.search(r'href="(https://rule34video\.com/video/\d+/[^"]*)"', html)
    if v_match:
        v_url = v_match.group(1)
        print(f"  Found video page: {v_url}")
        req2 = urllib.request.Request(v_url, headers={'User-Agent': UA})
        with urllib.request.urlopen(req2, timeout=5) as r2:
            v_html = r2.read().decode('utf-8', errors='ignore')
        s_match = re.search(r"video_url:\s*'([^']+)'", v_html)
        if s_match:
            stream_url = s_match.group(1)
            print(f"  Extracted stream: {stream_url}")
            ok, msg = test_probe(stream_url, {'Referer': 'https://rule34video.com/'})
            print(f"  Stream probe: {'SUCCESS' if ok else 'FAIL'} -> {msg}")
        else:
            print("  No video_url flashvar found")
    else:
        print("  No video links on latest-updates")
except Exception as e:
    print(f"  Rule34Video test failed: {e}")

# 2. HQPorner
print("\n[2/6] HQPORNER")
try:
    req = urllib.request.Request("https://hqporner.com/top/month", headers={'User-Agent': UA})
    with urllib.request.urlopen(req, timeout=5) as r:
        html = r.read().decode('utf-8', errors='ignore')
    v_match = re.search(r'href="(/hdporn/\d+-[^"]+\.html)"', html)
    if v_match:
        v_url = "https://hqporner.com" + v_match.group(1)
        print(f"  Found video page: {v_url}")
        req2 = urllib.request.Request(v_url, headers={'User-Agent': UA})
        with urllib.request.urlopen(req2, timeout=5) as r2:
            v_html = r2.read().decode('utf-8', errors='ignore')
        embed_m = re.search(r"url:\s*'/blocks/(?:nativeplayer|altplayer)\.php\?i=([^']+)'", v_html)
        if embed_m:
            embed_url = embed_m.group(1)
            if embed_url.startswith("//"): embed_url = "https:" + embed_url
            print(f"  Found embed iframe: {embed_url}")
            req3 = urllib.request.Request(embed_url, headers={'User-Agent': UA, 'Referer': 'https://hqporner.com/'})
            with urllib.request.urlopen(req3, timeout=5) as r3:
                e_html = r3.read().decode('utf-8', errors='ignore')
            cdn_m = re.search(r'(?://|https?://)([a-zA-Z0-9.-]+\.bigcdn\.cc/[^\\"\'>\s]+\.mp4)', e_html)
            if cdn_m:
                stream_url = "https://" + cdn_m.group(1)
                print(f"  Extracted stream: {stream_url}")
                ok, msg = test_probe(stream_url, {'Referer': 'https://hqporner.com/'})
                print(f"  Stream probe: {'SUCCESS' if ok else 'FAIL'} -> {msg}")
            else:
                print("  No bigcdn stream found in embed HTML")
        else:
            print("  No embed player parameter found")
    else:
        print("  No video links on top/month")
except Exception as e:
    print(f"  HQPorner test failed: {e}")

# 3. XVideos
print("\n[3/6] XVIDEOS")
try:
    req = urllib.request.Request("https://www.xvideos2.com/", headers={'User-Agent': UA})
    with urllib.request.urlopen(req, timeout=5) as r:
        html = r.read().decode('utf-8', errors='ignore')
    v_match = re.search(r'href="(/video\.[a-zA-Z0-9_\-]+/[^"]*|/video\d+/[^"]*)"', html)
    if v_match:
        v_url = "https://www.xvideos2.com" + v_match.group(1)
        print(f"  Found video page: {v_url}")
        req2 = urllib.request.Request(v_url, headers={'User-Agent': UA, 'Referer': 'https://www.xvideos2.com/'})
        with urllib.request.urlopen(req2, timeout=5) as r2:
            v_html = r2.read().decode('utf-8', errors='ignore')
        hls_m = re.search(r"html5player\.setVideoHLS\('([^']+)'\)", v_html)
        mp4_m = re.search(r"html5player\.setVideoUrlHigh\('([^']+)'\)", v_html)
        stream_url = hls_m.group(1) if hls_m else (mp4_m.group(1) if mp4_m else None)
        if stream_url:
            print(f"  Extracted stream: {stream_url}")
            ok, msg = test_probe(stream_url, {'Referer': 'https://www.xvideos2.com/'})
            print(f"  Stream probe: {'SUCCESS' if ok else 'FAIL'} -> {msg}")
        else:
            print("  No HLS or MP4 stream found in video HTML")
    else:
        print("  No video links on home page")
except Exception as e:
    print(f"  XVideos test failed: {e}")

# 4. Beeg
print("\n[4/6] BEEG")
try:
    req = urllib.request.Request("https://store.externulls.com/facts/tag?slug=index&limit=20&offset=0", headers={'User-Agent': UA, 'Referer': 'https://beeg.com/', 'Origin': 'https://beeg.com'})
    with urllib.request.urlopen(req, timeout=5) as r:
        b_data = json.loads(r.read().decode('utf-8'))
    if b_data:
        file_id = str(b_data[0]['file']['id'])
        print(f"  Found Beeg file ID: {file_id}")
        play_url = f"https://store.externulls.com/video/play_url/{file_id}"
        req2 = urllib.request.Request(play_url, headers={'User-Agent': UA, 'Referer': 'https://beeg.com/', 'Origin': 'https://beeg.com'})
        with urllib.request.urlopen(req2, timeout=5) as r2:
            hls_path = r2.read().decode('utf-8').strip()
        stream_url = hls_path if hls_path.startswith("http") else ("https://video.beeg.com/" + hls_path.lstrip('/'))
        print(f"  Extracted stream: {stream_url}")
        ok, msg = test_probe(stream_url, {'Referer': 'https://beeg.com/', 'Origin': 'https://beeg.com'})
        print(f"  Stream probe: {'SUCCESS' if ok else 'FAIL'} -> {msg}")
    else:
        print("  No items in store.externulls.com tag")
except Exception as e:
    print(f"  Beeg test failed: {e}")

# 5. Faphouse
print("\n[5/6] FAPHOUSE")
try:
    req = urllib.request.Request("https://faphouse.com/", headers={'User-Agent': UA, 'Referer': 'https://faphouse.com/'})
    with urllib.request.urlopen(req, timeout=5) as r:
        html = r.read().decode('utf-8', errors='ignore')
    v_links = re.findall(r'href="(/videos/[^"#]+)', html)
    valid_links = [l for l in v_links if not l.startswith('/videos/vr')]
    if valid_links:
        v_url = "https://faphouse.com" + valid_links[0]
        print(f"  Found video page: {v_url}")
        req2 = urllib.request.Request(v_url, headers={'User-Agent': UA, 'Referer': 'https://faphouse.com/'})
        with urllib.request.urlopen(req2, timeout=5) as r2:
            v_html = r2.read().decode('utf-8', errors='ignore')
        fb_m = re.search(r'data-fallback="([^"]+)"', v_html)
        src_m = re.search(r'<source[^>]+src="([^"]+)"', v_html)
        cdn_m = re.search(r'https?://video-nss\.fhcdngroup\.online/[^\s"\'<>]+', v_html)
        stream_url = fb_m.group(1) if fb_m else (src_m.group(1) if src_m else (cdn_m.group(0) if cdn_m else None))
        if stream_url:
            stream_url = stream_url.replace('&amp;', '&')
            print(f"  Extracted stream: {stream_url}")
            ok, msg = test_probe(stream_url, {'Referer': 'https://faphouse.com/'})
            print(f"  Stream probe: {'SUCCESS' if ok else 'FAIL'} -> {msg}")
        else:
            print("  No stream found in video HTML")
    else:
        print("  No valid video links on home page")
except Exception as e:
    print(f"  Faphouse test failed: {e}")

# 6. EPorner
print("\n[6/6] EPORNER (WITH DOH)")
try:
    ips = doh('www.eporner.com')
    unblocked = [ip for ip in ips if ip in ('94.75.220.4', '94.75.220.7')]
    target_ip = unblocked[0] if unblocked else ips[0]
    conn = http.client.HTTPSConnection(target_ip, 443, context=ctx, timeout=5)
    conn.request('GET', '/api/v2/video/search/?query=amateur&per_page=3&format=json', headers={
        'Host': 'www.eporner.com',
        'User-Agent': UA
    })
    res = conn.getresponse()
    data = json.loads(res.read().decode('utf-8'))
    videos = data.get('videos', [])
    if videos:
        v = videos[0]
        v_url = v['url']
        print(f"  Found video: {v['title']} ({v_url})")
        
        v_path = '/' + '/'.join(v_url.split('/')[3:])
        conn_p = http.client.HTTPSConnection(target_ip, 443, context=ctx, timeout=5)
        conn_p.request('GET', v_path, headers={'Host': 'www.eporner.com', 'User-Agent': UA})
        p_html = conn_p.getresponse().read().decode('utf-8', errors='ignore')
        
        dloads = re.findall(r'href="(/dload/[^"]+)"', p_html)
        if dloads:
            print(f"  Found {len(dloads)} dload quality options: {dloads[0]}")
            conn_d = http.client.HTTPSConnection(target_ip, 443, context=ctx, timeout=5)
            conn_d.request('GET', dloads[0], headers={
                'Host': 'www.eporner.com',
                'User-Agent': UA,
                'Referer': v_url
            })
            loc = conn_d.getresponse().getheader('Location')
            if loc:
                clean_stream_url = loc.split("?dload=")[0]
                print(f"  Extracted CDN stream: {clean_stream_url}")
                ok, msg = test_probe(clean_stream_url, {'Referer': 'https://www.eporner.com/'})
                print(f"  Stream probe: {'SUCCESS' if ok else 'FAIL'} -> {msg}")
            else:
                print("  No redirect Location returned for dload link")
        else:
            print("  No dload links found on page")
    else:
        print("  No videos returned by search API")
except Exception as e:
    print(f"  EPorner test failed: {e}")

print("\n" + "=" * 60)
print("AUDIT COMPLETE")
print("=" * 60)
