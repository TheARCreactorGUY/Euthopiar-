import sys
import re
import json
import urllib.request
import urllib.error
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.9"
}

def fetch_url(url, headers=None, referer=None):
    h = dict(HEADERS)
    if headers:
        h.update(headers)
    if referer:
        h["Referer"] = referer
    req = urllib.request.Request(url, headers=h)
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=12) as resp:
            return resp.status, resp.read().decode('utf-8', errors='ignore'), resp.headers
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', errors='ignore') if e.fp else "", e.headers
    except Exception as e:
        return -1, str(e), None

def probe_stream(url, headers=None, referer=None):
    h = dict(HEADERS)
    if headers:
        h.update(headers)
    if referer:
        h["Referer"] = referer
    h["Range"] = "bytes=0-1024"
    req = urllib.request.Request(url, headers=h)
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=10) as resp:
            content_type = resp.headers.get("Content-Type", "")
            return resp.status, content_type
    except urllib.error.HTTPError as e:
        return e.code, str(e)
    except Exception as e:
        return -1, str(e)

print("=" * 70)
print("TESTING ADULT PLUGINS LIVE STREAMING")
print("=" * 70)

results = {}

# 1. Beeg
print("\n--- 1. Testing Beeg ---")
try:
    code, body, _ = fetch_url("https://store.externulls.com/facts/tag?slug=index&limit=5&offset=0", referer="https://beeg.com/")
    if code == 200:
        data = json.loads(body)
        first_file = data[0]["file"]
        file_id = str(first_file["id"]).lstrip('0')
        print(f"Beeg sample file id: {file_id}")
        
        # Test play_url endpoint
        code_p, body_p, _ = fetch_url(f"https://store.externulls.com/video/play_url/{file_id}", referer="https://beeg.com/")
        print(f"Beeg play_url response code: {code_p}, body: {body_p[:100] if body_p else ''}")
        if code_p == 200 and body_p.strip():
            stream_path = body_p.strip()
            stream_url = stream_path if stream_path.startswith("http") else f"https://video.beeg.com/{stream_path}"
            p_code, p_type = probe_stream(stream_url, referer="https://beeg.com/")
            print(f"Beeg stream probe: HTTP {p_code}, Content-Type: {p_type}")
            results["Beeg"] = {"status": p_code in (200, 206), "code": p_code, "type": p_type, "url": stream_url}
        else:
            results["Beeg"] = {"status": False, "code": code_p, "error": f"play_url failed: {body_p[:100]}"}
    else:
        results["Beeg"] = {"status": False, "code": code, "error": "Catalog fetch failed"}
except Exception as e:
    results["Beeg"] = {"status": False, "error": str(e)}

# 2. Eporner
print("\n--- 2. Testing Eporner ---")
try:
    code, body, _ = fetch_url("https://www.eporner.com/api/v2/web/search/?query=top&per_page=5&page=1")
    if code == 200:
        data = json.loads(body)
        vid = data["videos"][0]
        vid_id = vid["id"]
        vid_url = vid["url"]
        print(f"Eporner video: {vid['title']} ({vid_id}) -> {vid_url}")
        
        # Fetch video page HTML
        code_page, html_page, _ = fetch_url(vid_url, referer="https://www.eporner.com/")
        # Find dload links
        dloads = re.findall(r'href="(/dload/[^"]+)"', html_page)
        print(f"Eporner dload paths found: {len(dloads)} -> {dloads[:3]}")
        if dloads:
            # Check redirect of dload
            dload_url = f"https://www.eporner.com{dloads[0]}"
            p_code, p_type = probe_stream(dload_url, referer=vid_url)
            print(f"Eporner probe {dload_url}: HTTP {p_code}, Content-Type: {p_type}")
            results["Eporner"] = {"status": p_code in (200, 206), "code": p_code, "type": p_type, "url": dload_url}
        else:
            # Check if there is an embed or direct MP4/HLS in the page
            mp4_links = re.findall(r'(https?://[^"\']+\.mp4[^\s"\']*)', html_page)
            print(f"Eporner mp4 matches: {mp4_links[:2]}")
            results["Eporner"] = {"status": False, "code": code_page, "error": "No dload paths found in HTML"}
    else:
        results["Eporner"] = {"status": False, "code": code, "error": "API failed"}
except Exception as e:
    results["Eporner"] = {"status": False, "error": str(e)}

# 3. Faphouse
print("\n--- 3. Testing Faphouse ---")
try:
    code, html, _ = fetch_url("https://faphouse.com/")
    print(f"Faphouse home code: {code}, html length: {len(html)}")
    video_links = re.findall(r'href="(/videos/[^"#]+)"', html)
    print(f"Faphouse video links: {video_links[:3]}")
    if video_links:
        v_url = f"https://faphouse.com{video_links[0]}"
        code_v, html_v, _ = fetch_url(v_url, referer="https://faphouse.com/")
        print(f"Faphouse video page {v_url}: HTTP {code_v}")
        
        # Check source patterns
        srcs = re.findall(r'<source[^>]+src="([^"]+)"', html_v)
        fallbacks = re.findall(r'data-fallback="([^"]+)"', html_v)
        fhcdns = re.findall(r'https?://video-nss\.fhcdngroup\.online/[^\s"\'<>]+', html_v)
        all_vids = re.findall(r'(https?://[^\s"\'<>]+\.(?:mp4|m3u8)[^\s"\'<>]*)', html_v)
        print(f"Faphouse <source>: {srcs}")
        print(f"Faphouse data-fallback: {fallbacks}")
        print(f"Faphouse fhcdn: {fhcdns}")
        print(f"Faphouse any mp4/m3u8: {all_vids[:3]}")
        
        stream_target = (srcs or fallbacks or fhcdns or all_vids or [None])[0]
        if stream_target:
            p_code, p_type = probe_stream(stream_target, referer=v_url)
            print(f"Faphouse probe: HTTP {p_code}, Content-Type: {p_type}")
            results["Faphouse"] = {"status": p_code in (200, 206), "code": p_code, "type": p_type, "url": stream_target}
        else:
            results["Faphouse"] = {"status": False, "code": code_v, "error": "No stream URLs found in video page HTML"}
    else:
        results["Faphouse"] = {"status": False, "code": code, "error": "No videos on homepage"}
except Exception as e:
    results["Faphouse"] = {"status": False, "error": str(e)}

# 4. HQPorner
print("\n--- 4. Testing HQPorner ---")
try:
    code, html, _ = fetch_url("https://hqporner.com/")
    print(f"HQPorner home code: {code}, html length: {len(html)}")
    video_links = re.findall(r'href="(/hdporn/[^"]+\.html)"', html)
    print(f"HQPorner video links: {video_links[:3]}")
    if video_links:
        v_url = f"https://hqporner.com{video_links[0]}"
        code_v, html_v, _ = fetch_url(v_url, referer="https://hqporner.com/")
        print(f"HQPorner video page: HTTP {code_v}")
        
        # Check embed parameter: class="player-iframe" or src="//... or player iframe
        embeds = re.findall(r'class="player-iframe"[^>]*src="([^"]+)"', html_v) or re.findall(r'<iframe[^>]+src="([^"]+)"', html_v)
        print(f"HQPorner embeds: {embeds}")
        if embeds:
            emb_url = embeds[0]
            if emb_url.startswith("//"):
                emb_url = f"https:{emb_url}"
            code_emb, html_emb, _ = fetch_url(emb_url, referer=v_url)
            print(f"HQPorner embed HTML: HTTP {code_emb}")
            sources = re.findall(r'<source[^>]+src="([^"]+)"[^>]*title="([^"]+)"', html_emb) or re.findall(r'<source[^>]+src="([^"]+)"', html_emb)
            print(f"HQPorner embed sources: {sources}")
            if sources:
                src_url = sources[0] if isinstance(sources[0], str) else sources[0][0]
                if src_url.startswith("//"):
                    src_url = f"https:{src_url}"
                p_code, p_type = probe_stream(src_url, referer=emb_url)
                print(f"HQPorner probe: HTTP {p_code}, Content-Type: {p_type}")
                results["HQPorner"] = {"status": p_code in (200, 206), "code": p_code, "type": p_type, "url": src_url}
            else:
                results["HQPorner"] = {"status": False, "code": code_emb, "error": "No sources in embed page"}
        else:
            results["HQPorner"] = {"status": False, "code": code_v, "error": "No embed iframe in page"}
    else:
        results["HQPorner"] = {"status": False, "code": code, "error": "No video links on homepage"}
except Exception as e:
    results["HQPorner"] = {"status": False, "error": str(e)}

# 5. Rule34Video
print("\n--- 5. Testing Rule34Video ---")
try:
    code, html, _ = fetch_url("https://rule34video.com/")
    print(f"Rule34Video home code: {code}, html length: {len(html)}")
    video_links = re.findall(r'href="(https?://rule34video\.com/video/\d+/[^"]*)"', html) or re.findall(r'href="(/video/\d+/[^"]*)"', html)
    print(f"Rule34Video video links: {video_links[:3]}")
    if video_links:
        v_url = video_links[0]
        if v_url.startswith("/"):
            v_url = f"https://rule34video.com{v_url}"
        code_v, html_v, _ = fetch_url(v_url, referer="https://rule34video.com/")
        print(f"Rule34Video video page: HTTP {code_v}")
        
        # Check flashvars
        v_urls = re.findall(r'video_url:\s*\'([^\']+)\'', html_v)
        v_alt_urls = re.findall(r'video_alt_url\d*:\s*\'([^\']+)\'', html_v)
        print(f"Rule34Video video_url: {v_urls}")
        print(f"Rule34Video video_alt_url: {v_alt_urls}")
        target = (v_urls or v_alt_urls or [None])[0]
        if target:
            p_code, p_type = probe_stream(target, referer=v_url)
            print(f"Rule34Video probe: HTTP {p_code}, Content-Type: {p_type}")
            results["Rule34Video"] = {"status": p_code in (200, 206), "code": p_code, "type": p_type, "url": target}
        else:
            results["Rule34Video"] = {"status": False, "code": code_v, "error": "No flashvars video_url found"}
    else:
        results["Rule34Video"] = {"status": False, "code": code, "error": "No video links on homepage"}
except Exception as e:
    results["Rule34Video"] = {"status": False, "error": str(e)}

# 6. Xvideos
print("\n--- 6. Testing Xvideos ---")
try:
    code, html, _ = fetch_url("https://www.xvideos.com/")
    print(f"Xvideos home code: {code}, html length: {len(html)}")
    video_links = re.findall(r'href="(/video\.[^/"]+/[^"]*)"', html) or re.findall(r'href="(/video\d+/[^"]*)"', html)
    print(f"Xvideos video links: {video_links[:3]}")
    if video_links:
        v_url = f"https://www.xvideos.com{video_links[0]}"
        code_v, html_v, _ = fetch_url(v_url, referer="https://www.xvideos.com/")
        print(f"Xvideos video page: HTTP {code_v}")
        
        # Check setVideoHLS, setVideoUrlHigh, setVideoUrlLow
        hls_matches = re.findall(r'html5player\.setVideoHLS\(\'([^\']+)\'\)', html_v)
        high_matches = re.findall(r'html5player\.setVideoUrlHigh\(\'([^\']+)\'\)', html_v)
        low_matches = re.findall(r'html5player\.setVideoUrlLow\(\'([^\']+)\'\)', html_v)
        print(f"Xvideos HLS: {hls_matches}")
        print(f"Xvideos High MP4: {high_matches}")
        print(f"Xvideos Low MP4: {low_matches}")
        
        target = (hls_matches or high_matches or low_matches or [None])[0]
        if target:
            p_code, p_type = probe_stream(target, referer=v_url)
            print(f"Xvideos probe: HTTP {p_code}, Content-Type: {p_type}")
            results["Xvideos"] = {"status": p_code in (200, 206), "code": p_code, "type": p_type, "url": target}
        else:
            results["Xvideos"] = {"status": False, "code": code_v, "error": "No html5player links found"}
    else:
        results["Xvideos"] = {"status": False, "code": code, "error": "No video links on homepage"}
except Exception as e:
    results["Xvideos"] = {"status": False, "error": str(e)}

print("\n" + "=" * 70)
print("FINAL SUMMARY OF ADULT PROVIDERS")
print("=" * 70)
for name, res in results.items():
    st = "WORKING" if res.get("status") else "FAILED"
    print(f"  {name:15}: {st} -> {res}")
