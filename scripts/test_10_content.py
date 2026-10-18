import urllib.request
import urllib.parse
import ssl
import re
import json
import base64
import time

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
    'Accept-Language': 'en-US,en;q=0.5',
}

def rot13(s):
    res = []
    for c in s:
        if 'a' <= c <= 'z':
            res.append(chr(((ord(c) - ord('a') + 13) % 26) + ord('a')))
        elif 'A' <= c <= 'Z':
            res.append(chr(((ord(c) - ord('A') + 13) % 26) + ord('A')))
        else:
            res.append(c)
    return "".join(res)

def bypass_greenmotors(greenmotors_url):
    try:
        req = urllib.request.Request(greenmotors_url, headers=HEADERS)
        resp = urllib.request.urlopen(req, context=ctx, timeout=12)
        html = resp.read().decode('utf-8', errors='ignore')
        
        # Look for s('o', 'PAYLOAD', 180*1000)
        m = re.search(r"s\(\s*['\"]o['\"]\s*,\s*['\"]([^'\"]+)['\"]\s*,\s*\d+", html)
        if not m:
            # Maybe direct redirect
            return None
        
        payload = m.group(1)
        s = base64.b64decode(payload).decode('utf-8', errors='ignore')
        s = base64.b64decode(s).decode('utf-8', errors='ignore')
        s = rot13(s)
        s = base64.b64decode(s).decode('utf-8', errors='ignore')
        data = json.loads(s)
        dest_b64 = data.get('o', '')
        dest_url = base64.b64decode(dest_b64).decode('utf-8', errors='ignore')
        return dest_url
    except Exception as e:
        return f"Error: {e}"

def resolve_hubcloud(hubcloud_url):
    try:
        req = urllib.request.Request(hubcloud_url, headers={**HEADERS, 'Referer': 'https://greenmotors.cc/'})
        resp = urllib.request.urlopen(req, context=ctx, timeout=12)
        html = resp.read().decode('utf-8', errors='ignore')
        
        # Find gamerxyt url in script or link
        m = re.search(r"var\s+url\s*=\s*['\"](https://gamerxyt\.com/hubcloud\.php[^'\"]+)['\"]", html)
        if not m:
            m = re.search(r"href=['\"](https://gamerxyt\.com/hubcloud\.php[^'\"]+)['\"]", html)
        if not m:
            return None
        return m.group(1)
    except Exception as e:
        return f"Error: {e}"

def resolve_gamerxyt(gamerxyt_url):
    try:
        req = urllib.request.Request(gamerxyt_url, headers={**HEADERS, 'Referer': 'https://hubcloud.ist/'})
        resp = urllib.request.urlopen(req, context=ctx, timeout=12)
        html = resp.read().decode('utf-8', errors='ignore')
        
        servers = []
        links = re.findall(r'<a[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)
        for href, text in links:
            clean_text = re.sub(r'<[^>]+>', '', text).strip()
            if any(k in clean_text.lower() for k in ['download', 'watch online', 'server', 'fsl', '10gbps', 'buzz']):
                if not any(x in href.lower() for x in ['telegram', 'winexch', 'tinyurl', 'bonuscaf', 'google']):
                    servers.append({
                        'name': clean_text,
                        'url': href
                    })
        return servers
    except Exception as e:
        return []

def test_media_link(url):
    try:
        req = urllib.request.Request(url, headers={**HEADERS, 'Range': 'bytes=0-100'})
        resp = urllib.request.urlopen(req, context=ctx, timeout=8)
        content_range = resp.headers.get('Content-Range') or ''
        content_type = resp.headers.get('Content-Type') or ''
        data = resp.read()
        return {
            'status': resp.status,
            'range': content_range,
            'type': content_type,
            'magic_hex': data[:8].hex() if data else ''
        }
    except Exception as e:
        return {'error': str(e)}

TEST_ITEMS = [
    {
        'title': "Marvel's The Punisher",
        'type': "Series",
        'url': "https://4khdhub.one/marvels-the-punisher-series-2288/"
    },
    {
        'title': "Death of a Unicorn",
        'type': "Movie",
        'url': "https://4khdhub.one/death-of-a-unicorn-movie-8212/"
    },
    {
        'title': "MobLand",
        'type': "Series",
        'url': "https://4khdhub.one/mobland-series-98/"
    },
    {
        'title': "UNABOMBER",
        'type': "Movie",
        'url': "https://4khdhub.one/unabomber-movie-8202/"
    },
    {
        'title': "The Final Problem",
        'type': "Series",
        'url': "https://4khdhub.one/the-final-problem-series-8203/"
    },
    {
        'title': "A Different World",
        'type': "Series",
        'url': "https://4khdhub.one/a-different-world-series-8184/"
    },
    {
        'title': "Toy Story 5",
        'type': "Movie",
        'url': "https://4khdhub.one/toy-story-5-movie-8178/"
    },
    {
        'title': "The Love Hypothesis",
        'type': "Movie",
        'url': "https://4khdhub.one/the-love-hypothesis-movie-8179/"
    },
    {
        'title': "The Get Out",
        'type': "Movie",
        'url': "https://4khdhub.one/the-get-out-movie-8139/"
    },
    {
        'title': "Space Jam",
        'type': "Movie",
        'url': "https://4khdhub.one/space-jam-movie-8222/"
    },
]

results = []

print("=== STARTING 10 CONTENT ANALYSIS ON 4KHDHUB ===")

for idx, item in enumerate(TEST_ITEMS, start=1):
    print(f"\n[{idx}/10] Testing: {item['title']} ({item['type']})")
    print(f"URL: {item['url']}")
    
    try:
        req = urllib.request.Request(item['url'], headers=HEADERS)
        resp = urllib.request.urlopen(req, context=ctx, timeout=12)
        html = resp.read().decode('utf-8', errors='ignore')
    except Exception as e:
        print(f"Failed to fetch content page: {e}")
        continue
    
    # 1. Check embedded online stream player (videasy/autoembed)
    tmdb_match = re.search(r"defaultVideoId\s*=\s*['\"](\d+)['\"]", html)
    tmdb_id = tmdb_match.group(1) if tmdb_match else None
    
    online_streams = []
    if tmdb_id:
        if item['type'] == 'Movie':
            online_streams.append(f"https://player.videasy.net/movie/{tmdb_id}")
            online_streams.append(f"https://player.autoembed.cc/embed/movie/{tmdb_id}")
        else:
            online_streams.append(f"https://player.videasy.net/tv/{tmdb_id}/1/1")
            online_streams.append(f"https://player.autoembed.cc/embed/tv/{tmdb_id}/1/1")
    
    # 2. Extract Download/Stream Cards & Greenmotors links
    # Search for all greenmotors links
    gm_links = re.findall(r'href=["\'](https://greenmotors\.club/\?id=[^"\']+)["\']', html)
    
    # Also find titles / qualities
    file_titles = re.findall(r'class="file-title">([^<]+)<', html)
    if not file_titles:
        file_titles = re.findall(r'class="episode-file-title">([^<]+)<', html)
    
    print(f"Found TMDB ID: {tmdb_id}")
    print(f"Online Player Streams: {len(online_streams)}")
    print(f"Greenmotors Raw Links: {len(gm_links)}")
    print(f"Available File Variants: {len(file_titles)}")
    
    resolved_sources = []
    verified_playback = []
    
    # Test resolving the first 2 greenmotors links to verify bypass
    sample_links = gm_links[:2]
    for g_idx, g_url in enumerate(sample_links):
        hubcloud_url = bypass_greenmotors(g_url)
        if hubcloud_url and not hubcloud_url.startswith("Error"):
            gamer_url = resolve_hubcloud(hubcloud_url)
            if gamer_url and not gamer_url.startswith("Error"):
                servers = resolve_gamerxyt(gamer_url)
                resolved_sources.extend(servers)
                # Verify first direct server
                for s in servers:
                    if 'r2.cloudflarestorage.com' in s['url'] or 'gpdl' in s['url']:
                        probe = test_media_link(s['url'])
                        verified_playback.append({
                            'name': s['name'],
                            'probe': probe
                        })
                        break
    
    item_result = {
        'title': item['title'],
        'type': item['type'],
        'tmdb_id': tmdb_id,
        'online_streams': online_streams,
        'variants_count': len(file_titles) or len(gm_links),
        'sample_variants': file_titles[:3],
        'resolved_servers_count': len(resolved_sources),
        'sample_servers': [s['name'] for s in resolved_sources[:4]],
        'verified_playback': verified_playback
    }
    results.append(item_result)
    print(f"  -> Resolved {len(resolved_sources)} direct servers, Verified Direct Media: {len(verified_playback)}")
    time.sleep(1)

with open('scripts/test_results.json', 'w', encoding='utf-8') as f:
    json.dump(results, f, indent=2)

print("\n=== FINISHED TESTING 10 CONTENT ITEMS ===")
print("Results saved to scripts/test_results.json")
