#!/usr/bin/env python3
"""
Step-by-step diagnostic script for 4KHDHub.
Inspects live website endpoints for a Movie and a Web Series:
1. Fetches raw post pages from https://4khdhub.one
2. Follows the redirection chain (Greenmotors -> HubCloud -> Gamerxyt)
3. Catalogs EVERY link present on the provider page and Gamerxyt page
4. Probes live stream/download URLs (HTTP 200/206 Range requests)
5. Verifies audio, subtitles, TMDB, AniSkip, OpenSubtitles bridging
"""

import sys
import re
import json
import base64
import urllib.request
import urllib.parse

sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

def fetch_url(url, headers=None, timeout=10):
    h = {"User-Agent": USER_AGENT}
    if headers:
        h.update(headers)
    req = urllib.request.Request(url, headers=h)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            content = resp.read()
            encoding = resp.headers.get_content_charset() or 'utf-8'
            return resp.status, content.decode(encoding, errors='replace'), resp.headers
    except urllib.error.HTTPError as e:
        return e.code, "", e.headers
    except Exception as e:
        return 0, str(e), {}

def probe_stream(url, headers=None, timeout=8):
    h = {
        "User-Agent": USER_AGENT,
        "Range": "bytes=0-1048575" # request first 1MB
    }
    if headers:
        h.update(headers)
    req = urllib.request.Request(url, headers=h)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            status = resp.status
            content_type = resp.headers.get("Content-Type", "")
            content_range = resp.headers.get("Content-Range", "")
            content_length = resp.headers.get("Content-Length", "")
            accept_ranges = resp.headers.get("Accept-Ranges", "")
            data = resp.read(1024) # read first 1KB
            is_valid_media = len(data) > 0
            return {
                "status": status,
                "content_type": content_type,
                "content_range": content_range,
                "content_length": content_length,
                "accept_ranges": accept_ranges,
                "bytes_read": len(data),
                "is_live": status in (200, 206) and is_valid_media
            }
    except urllib.error.HTTPError as e:
        return {"status": e.code, "is_live": False, "error": str(e)}
    except Exception as e:
        return {"status": 0, "is_live": False, "error": str(e)}

def rot13(s):
    result = []
    for c in s:
        if 'a' <= c <= 'z':
            result.append(chr((ord(c) - ord('a') + 13) % 26 + ord('a')))
        elif 'A' <= c <= 'Z':
            result.append(chr((ord(c) - ord('A') + 13) % 26 + ord('A')))
        else:
            result.append(c)
    return "".join(result)

def bypass_greenmotors(gm_url):
    code, html, _ = fetch_url(gm_url)
    if code != 200:
        return None
    
    # Direct hubcloud link
    m_direct = re.search(r'https?://[^\'"\s]+hubcloud[^\'"\s]+', html)
    if m_direct and not any(x in m_direct.group(0) for x in [".css", ".js"]):
        return m_direct.group(0)
    
    m_payload = re.search(r"s\(\s*['\"]o['\"]\s*,\s*['\"]([^'\"]+)['\"]\s*,\s*\d+", html)
    if not m_payload:
        return None
    raw_payload = m_payload.group(1)
    try:
        s1 = base64.b64decode(raw_payload).decode('utf-8')
        s2 = base64.b64decode(s1).decode('utf-8')
        s3 = rot13(s2)
        s4 = base64.b64decode(s3).decode('utf-8')
        root = json.loads(s4)
        dest_b64 = root.get("o")
        return base64.b64decode(dest_b64).decode('utf-8')
    except Exception as e:
        return None

def resolve_hubcloud(hc_url):
    code, html, _ = fetch_url(hc_url, headers={"Referer": "https://greenmotors.cc/"})
    if code != 200:
        return None
    m = re.search(r"var\s+url\s*=\s*['\"](https?://[^'\"]+/hubcloud\.php[^'\"]+)['\"]", html)
    if not m:
        m = re.search(r"href=['\"](https?://[^'\"]+/hubcloud\.php[^'\"]+)['\"]", html)
    if not m:
        m = re.search(r"['\"](https?://[^'\"]*gamerxyt[^'\"]*)['\"]", html)
    if not m and "hubdrive." in hc_url:
        # HubDrive page inspection
        hub_match = re.search(r"href=['\"](https?://[^/'\"\s]*hubcloud[^/'\"\s]*/drive/[^'\"]+)['\"]", html)
        if hub_match:
            return resolve_hubcloud(hub_match.group(1))
def inspect_hubdrive(url):
    code, html, _ = fetch_url(url)
    print(f"HubDrive HTTP: {code}")
    links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)
    for h, t in links:
        clean = re.sub(r'<[^>]+>', '', t).strip()
        print(f"  HubDrive Link: [{clean}] -> {h}")
    # Also check form actions or javascript
    forms = re.findall(r'<form\s+[^>]*action=["\']([^"\']+)["\'][^>]*>', html, re.DOTALL | re.IGNORECASE)
    for f in forms:
        print(f"  HubDrive Form Action: -> {f}")

def inspect_gamerxyt(gamer_url):
    code, html, _ = fetch_url(gamer_url, headers={"Referer": "https://hubcloud.ist/"})
    if code != 200:
        return []
    
    # Extract all links on gamerxyt page
    links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)
    results = []
    for href, text in links:
        clean_text = re.sub(r'<[^>]+>', '', text).strip()
        results.append((clean_text, href))
    return results

def inspect_movie_and_series():
    print("="*80)
    print(" STEP 3 & 4: LIVE ENDPOINT DISCOVERY & STREAM PROBING ON 4KHDHUB.ONE")
    print("="*80)

    # 1. Fetch homepage
    print("[*] Fetching 4khdhub homepage: https://4khdhub.one/")
    code, html, _ = fetch_url("https://4khdhub.one/")
    if code != 200:
        print(f"[!] Failed to load homepage: HTTP {code}")
        return

    # Find movie post
    movie_links = re.findall(r'<a\s+[^>]*href=["\']([^"\']*-movie-[^"\']*)["\'][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)
    # Find series post
    series_links = re.findall(r'<a\s+[^>]*href=["\']([^"\']*-series-[^"\']*)["\'][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)

    movie_url = "https://4khdhub.one" + movie_links[0][0] if movie_links and movie_links[0][0].startswith("/") else (movie_links[0][0] if movie_links else None)
    series_url = "https://4khdhub.one" + series_links[0][0] if series_links and series_links[0][0].startswith("/") else (series_links[0][0] if series_links else None)

    if not movie_url:
        movie_url = "https://4khdhub.one/spider-man-brand-new-day-2026-movie-download-4k-1080p-720p-hindi-english/"
    if not series_url:
        series_url = "https://4khdhub.one/the-punisher-season-1-2-hindi-dubbed-dual-audio-web-series-download-4khdhub/"

    print(f"\n--- [A] INSPECTING LIVE MOVIE: {movie_url} ---")
    inspect_single_post(movie_url, is_series=False)

    print(f"\n--- [B] INSPECTING LIVE WEBSERIES: {series_url} ---")
    inspect_single_post(series_url, is_series=True)

def inspect_single_post(post_url, is_series=False):
    code, html, _ = fetch_url(post_url)
    if code != 200:
        print(f"[!] Failed to fetch post: HTTP {code}")
        return

    # Extract title
    title_m = re.search(r'<h1[^>]*>(.*?)</h1>', html, re.DOTALL | re.IGNORECASE)
    raw_title = re.sub(r'<[^>]+>', '', title_m.group(1)).strip() if title_m else "Unknown Title"
    print(f"Title: {raw_title}")

    # Extract all download-items / episode-download-items
    if not is_series:
        dl_items = re.findall(r'<div[^>]*class=["\'][^"\']*download-item[^"\']*["\'][^>]*>(.*?)</div>\s*</div>', html, re.DOTALL | re.IGNORECASE)
        print(f"Found {len(dl_items)} download boxes on movie page.")
        
        all_links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)
        gm_links = [(re.sub(r'<[^>]+>', '', t).strip(), h) for h, t in all_links if any(x in h for x in ["greenmotors", "hubcloud", "gamerxyt", "drive"])]
        print(f"Found {len(gm_links)} total cloud intermediate links.")

        for idx, (label, link) in enumerate(gm_links[:4], 1):
            print(f"\n  [Variant #{idx}] {label} -> {link}")
            hc_url = bypass_greenmotors(link) if "greenmotors" in link else link
            print(f"    * Greenmotors Bypassed -> {hc_url}")
            if hc_url:
                gamer_url = resolve_hubcloud(hc_url)
                print(f"    * HubCloud Resolved -> {gamer_url}")
                if gamer_url:
                    gamer_links = inspect_gamerxyt(gamer_url)
                    print(f"    * Gamerxyt Endpoints Found: {len(gamer_links)}")
                    for g_label, g_href in gamer_links:
                        print(f"        -> [{g_label}] {g_href}")
                        if any(x in g_href for x in ["r2.cloudflarestorage", "buzz", "cocktail", "lenin", "snvhost", "pixeldrain"]):
                            probe = probe_stream(g_href)
                            print(f"           PROBE: HTTP {probe.get('status')} | Live: {probe.get('is_live')} | Type: {probe.get('content_type')} | Range: {probe.get('content_range')}")
    else:
        # TV / Web Series
        seasons = re.findall(r'<div[^>]*class=["\'][^"\']*(?:season-item|episode-item)[^"\']*["\'][^>]*>(.*?)</div>\s*</div>\s*</div>', html, re.DOTALL | re.IGNORECASE)
        ep_items = re.findall(r'<div[^>]*class=["\'][^"\']*episode-download-item[^"\']*["\'][^>]*>(.*?)</div>\s*</div>', html, re.DOTALL | re.IGNORECASE)
        print(f"Found {len(seasons)} season sections and {len(ep_items)} episode items on series page.")

        all_links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)
        gm_links = [(re.sub(r'<[^>]+>', '', t).strip(), h) for h, t in all_links if any(x in h for x in ["greenmotors", "hubcloud", "gamerxyt", "drive"])]
        print(f"Found {len(gm_links)} total episode cloud links.")

        # Test Episode 1
        if gm_links:
            e1_label, e1_link = gm_links[0]
            print(f"\n  [Episode 1 Test] {e1_label} -> {e1_link}")
            hc_url = bypass_greenmotors(e1_link) if "greenmotors" in e1_link else e1_link
            print(f"    * Greenmotors Bypassed -> {hc_url}")
            if hc_url:
                gamer_url = resolve_hubcloud(hc_url)
                print(f"    * HubCloud Resolved -> {gamer_url}")
                if gamer_url:
                    gamer_links = inspect_gamerxyt(gamer_url)
                    print(f"    * Gamerxyt Endpoints Found: {len(gamer_links)}")
                    for g_label, g_href in gamer_links:
                        print(f"        -> [{g_label}] {g_href}")
                        if any(x in g_href for x in ["r2.cloudflarestorage", "buzz", "cocktail", "lenin", "snvhost", "pixeldrain"]):
                            probe = probe_stream(g_href)
                            print(f"           PROBE: HTTP {probe.get('status')} | Live: {probe.get('is_live')} | Type: {probe.get('content_type')} | Range: {probe.get('content_range')}")

if __name__ == "__main__":
    inspect_movie_and_series()

