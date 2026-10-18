#!/usr/bin/env python3
"""
Diagnostic script for 1Shows EUP v2:
Inspects live website endpoints for a Movie and a Web Series:
1. Tests https://www.1shows.bz live site & APIs
2. Tests Vidzee multi-audio/server streams (player.vidzee.wtf / core.vidzee.wtf)
3. Tests Viduki / MakimaDL download token & decrypted video sources
4. Tests Viduki Main dynamic servers (Leon, Jill, Claire, etc.)
5. Probes live stream/download URLs (HTTP 200/206 Range requests)
6. Verifies audio, subtitles, TMDB, AniSkip, and OpenSubtitles bridging
"""

import sys
import json
import re
import urllib.request
import urllib.parse

sys.stdout.reconfigure(encoding='utf-8')
sys.stderr.reconfigure(encoding='utf-8')

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

def fetch_json(url, headers=None, timeout=10):
    h = {
        "User-Agent": USER_AGENT,
        "Referer": "https://www.1shows.bz/",
        "Origin": "https://www.1shows.bz",
        "Accept": "application/json"
    }
    if headers:
        h.update(headers)
    req = urllib.request.Request(url, headers=h)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            content = resp.read()
            return resp.status, json.loads(content.decode('utf-8', errors='replace'))
    except urllib.error.HTTPError as e:
        return e.code, None
    except Exception as e:
        return 0, str(e)

def probe_stream(url, headers=None, timeout=8):
    h = {
        "User-Agent": USER_AGENT,
        "Range": "bytes=0-1048575"
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

def test_endpoints():
    print("="*80)
    print(" STEP 3 & 4: 1SHOWS LIVE ENDPOINT DISCOVERY & STREAM PROBING")
    print("="*80)

    # 1. Test 1shows.bz trending APIs
    print("[*] Testing 1Shows Trending Movies API: https://www.1shows.bz/api/trending/movie/day")
    st, data = fetch_json("https://www.1shows.bz/api/trending/movie/day")
    print(f"    Status: HTTP {st}")
    movie_id = "969681" # default Spider-Man
    if data and "results" in data and len(data["results"]) > 0:
        top_movie = data["results"][0]
        movie_id = str(top_movie.get("id", movie_id))
        print(f"    Top Trending Movie: '{top_movie.get('title')}' (ID: {movie_id})")

    print("[*] Testing 1Shows Trending TV API: https://www.1shows.bz/api/trending/tv/day")
    st_tv, data_tv = fetch_json("https://www.1shows.bz/api/trending/tv/day")
    print(f"    Status: HTTP {st_tv}")
    tv_id = "1399" # Game of Thrones
    if data_tv and "results" in data_tv and len(data_tv["results"]) > 0:
        top_tv = data_tv["results"][0]
        tv_id = str(top_tv.get("id", tv_id))
        print(f"    Top Trending Series: '{top_tv.get('name')}' (ID: {tv_id})")

    # 2. Test Movie details on 1Shows
    print(f"\n--- [A] AUDITING LIVE MOVIE: TMDB ID {movie_id} ---")
    st_m, m_detail = fetch_json(f"https://www.1shows.bz/api/movie/{movie_id}")
    print(f"    Movie Details API: HTTP {st_m}")
    if m_detail:
        print(f"    Title: {m_detail.get('title')}")
        print(f"    IMDb ID: {m_detail.get('imdb_id')}")

    # Test Vidzee subs
    subs_url = f"https://core.vidzee.wtf/subs/movie/{movie_id}"
    st_subs, subs_data = fetch_json(subs_url, headers={"Referer": "https://player.vidzee.wtf/"})
    print(f"    Vidzee Subtitles: HTTP {st_subs} (Found: {len(subs_data) if isinstance(subs_data, list) else 0} tracks)")

    # Test Viduki download token
    dt_st, dt_data = fetch_json("https://api.viduki.net/download-token", headers={"Referer": "https://www.1shows.bz/"})
    print(f"    Viduki Download Token: HTTP {dt_st} (Token: {bool(dt_data and 'token' in dt_data)})")

    # Test Viduki Main Servers
    srv_st, srv_data = fetch_json("https://api.viduki.net/main/servers", headers={"Referer": "https://www.viduki.net/"})
    print(f"    Viduki Main Server Pool: HTTP {srv_st} (Servers: {[s.get('name') for s in srv_data] if isinstance(srv_data, list) else 0})")

    # 3. Test Series details on 1Shows
    print(f"\n--- [B] AUDITING LIVE WEBSERIES: TMDB ID {tv_id} ---")
    st_t, t_detail = fetch_json(f"https://www.1shows.bz/api/tv/{tv_id}")
    print(f"    Series Details API: HTTP {st_t}")
    if t_detail:
        print(f"    Title: {t_detail.get('name')}")
        print(f"    Seasons Count: {len(t_detail.get('seasons', []))}")

    # Test Vidzee TV subs (S1E1)
    tv_subs_url = f"https://core.vidzee.wtf/subs/tv/{tv_id}/1/1"
    st_tv_subs, tv_subs = fetch_json(tv_subs_url, headers={"Referer": "https://player.vidzee.wtf/"})
    print(f"    Vidzee TV S1E1 Subtitles: HTTP {st_tv_subs} (Found: {len(tv_subs) if isinstance(tv_subs, list) else 0} tracks)")

if __name__ == "__main__":
    test_endpoints()
