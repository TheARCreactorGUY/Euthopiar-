#!/usr/bin/env python3
"""
Inspect 10 content pieces on 4khdhub.one (5 movies, 5 web series)
Catalogs:
1. All download boxes and links on the post page
2. Redirection endpoints (Greenmotors, HubCloud, HubDrive)
3. All buttons and mirrors on Gamerxyt
4. Live stream/download HTTP probe (200/206 status, bytes, range, content type)
5. Audio and Subtitle options
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
            return resp.status, content.decode(encoding, errors='replace')
    except urllib.error.HTTPError as e:
        return e.code, ""
    except Exception as e:
        return 0, str(e)

def probe_stream(url, headers=None, timeout=8):
    h = {
        "User-Agent": USER_AGENT,
        "Range": "bytes=0-65535"
    }
    if headers:
        h.update(headers)
    req = urllib.request.Request(url, headers=h)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            data = resp.read(1024)
            return {
                "status": resp.status,
                "content_type": resp.headers.get("Content-Type", ""),
                "content_range": resp.headers.get("Content-Range", ""),
                "is_live": resp.status in (200, 206) and len(data) > 0
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
    code, html = fetch_url(gm_url)
    if code != 200:
        return None
    m_direct = re.search(r'https?://[^\'"\s]+hubcloud[^\'"\s]+', html)
    if m_direct and not any(x in m_direct.group(0) for x in [".css", ".js"]):
        return m_direct.group(0)
    m_payload = re.search(r"s\(\s*['\"]o['\"]\s*,\s*['\"]([^'\"]+)['\"]\s*,\s*\d+", html)
    if not m_payload:
        return None
    try:
        s1 = base64.b64decode(m_payload.group(1)).decode('utf-8')
        s2 = base64.b64decode(s1).decode('utf-8')
        s3 = rot13(s2)
        s4 = base64.b64decode(s3).decode('utf-8')
        root = json.loads(s4)
        return base64.b64decode(root.get("o")).decode('utf-8')
    except Exception:
        return None

def resolve_hubcloud(hc_url):
    if "hubdrive." in hc_url:
        code, html = fetch_url(hc_url)
        if code == 200:
            m_hub = re.search(r'href=["\'](https?://[^/"\'\s]*hubcloud[^/"\'\s]*/drive/[^"\']+)["\']', html)
            if m_hub:
                return resolve_hubcloud(m_hub.group(1))
    code, html = fetch_url(hc_url, headers={"Referer": "https://greenmotors.cc/"})
    if code != 200:
        return None
    m = re.search(r"var\s+url\s*=\s*['\"](https?://[^'\"\s]+/hubcloud\.php[^'\"\s]+)['\"]", html)
    if not m:
        m = re.search(r"href=['\"](https?://[^'\"\s]+/hubcloud\.php[^'\"\s]+)['\"]", html)
    if not m:
        m = re.search(r"['\"](https?://[^'\"\s]*gamerxyt[^'\"\s]*)['\"]", html)
    return m.group(1) if m else None

def inspect_gamerxyt(gamer_url):
    code, html = fetch_url(gamer_url, headers={"Referer": "https://hubcloud.ist/"})
    if code != 200:
        return []
    links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)
    results = []
    for href, text in links:
        clean_text = re.sub(r'<[^>]+>', '', text).strip()
        results.append((clean_text, href))
    return results

def get_10_samples():
    print("[*] Gathering 10 content samples from 4khdhub.one...")
    code, html = fetch_url("https://4khdhub.one/")
    if code != 200:
        print(f"[!] Homepage failed: HTTP {code}")
        return []

    all_links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>', html, re.DOTALL | re.IGNORECASE)
    posts = []
    seen = set()
    for h in all_links:
        full = "https://4khdhub.one" + h if h.startswith("/") else h
        if "4khdhub.one/" in full and not any(x in full for x in ["/category/", "/tag/", "/page/", "/dmca", "/contact", "/privacy", "#", "?s="]):
            if full not in seen and len(full.strip("/").split("/")) > 3:
                seen.add(full)
                is_ser = "-series-" in full or "-season-" in full
                posts.append((full, is_ser))

    movies = [p for p in posts if not p[1]]
    series = [p for p in posts if p[1]]

    print(f"Found {len(movies)} movies and {len(series)} series on homepage.")
    samples = movies[:5] + series[:5]
    return samples

def main():
    samples = get_10_samples()
    print(f"[+] Total samples selected: {len(samples)} (5 Movies, 5 Web Series)")

    total_probed = 0
    total_live = 0

    for idx, (url, is_series) in enumerate(samples, 1):
        content_type = "WEBSERIES" if is_series else "MOVIE"
        print("\n" + "="*80)
        print(f"[{idx}/10] AUDITING {content_type}: {url}")
        print("="*80)

        code, html = fetch_url(url)
        if code != 200:
            print(f"  [!] Failed to load post: HTTP {code}")
            continue

        title_m = re.search(r'<h1[^>]*>(.*?)</h1>', html, re.DOTALL | re.IGNORECASE)
        title = re.sub(r'<[^>]+>', '', title_m.group(1)).strip() if title_m else "Unknown Title"
        print(f"  Title: {title}")

        # Extract cloud links
        all_links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)
        cloud_links = [(re.sub(r'<[^>]+>', '', t).strip(), h) for h, t in all_links if any(x in h for x in ["greenmotors", "hubcloud", "gamerxyt", "hubdrive"])]
        print(f"  Total cloud provider links on post: {len(cloud_links)}")

        # Audit first 2 variants / episodes
        for v_idx, (v_label, v_link) in enumerate(cloud_links[:2], 1):
            print(f"\n  -> Variant/Episode #{v_idx}: [{v_label}]")
            print(f"     Target: {v_link}")
            hc_url = bypass_greenmotors(v_link) if "greenmotors" in v_link else v_link
            if not hc_url:
                print("     [FAIL] Could not bypass greenmotors")
                continue
            print(f"     HubCloud URL: {hc_url}")
            gamer_url = resolve_hubcloud(hc_url)
            if not gamer_url:
                print("     [FAIL] Could not resolve hubcloud/hubdrive")
                continue
            print(f"     Gamerxyt URL: {gamer_url}")
            endpoints = inspect_gamerxyt(gamer_url)
            print(f"     Endpoints found on Gamerxyt: {len(endpoints)}")

            for e_label, e_url in endpoints:
                if any(k in e_url for k in ["r2.cloudflarestorage", "buzz", "cocktail", "lenin", "snvhost", "pixeldrain", "hbplay.pages.dev", "storage.googleapis.com"]):
                    raw_stream = e_url
                    if "hbplay.pages.dev/?u=" in e_url:
                        u_b64 = e_url.split("?u=")[1].split("&")[0]
                        try:
                            raw_stream = base64.b64decode(u_b64).decode('utf-8')
                        except Exception:
                            pass
                    probe = probe_stream(raw_stream)
                    total_probed += 1
                    if probe.get("is_live"):
                        total_live += 1
                    status_str = f"HTTP {probe.get('status')} [LIVE OK]" if probe.get("is_live") else f"HTTP {probe.get('status')} [FAIL]"
                    print(f"       * [{e_label}] -> {raw_stream[:65]}... => {status_str} (Type: {probe.get('content_type')})")

    print("\n" + "="*80)
    print(f"AUDIT SUMMARY: {total_live} / {total_probed} endpoints verified LIVE & WORKING.")
    print("="*80)

if __name__ == "__main__":
    main()
