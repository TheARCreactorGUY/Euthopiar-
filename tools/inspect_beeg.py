import urllib.request
import ssl
import json

ctx = ssl._create_unverified_context()
url = "https://store.externulls.com/facts/tag?slug=index&limit=20&offset=0"
req = urllib.request.Request(url, headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Referer': 'https://beeg.com/',
    'Origin': 'https://beeg.com',
    'Accept': 'application/json, text/plain, */*'
})
with urllib.request.urlopen(req, context=ctx, timeout=10) as r:
    data = json.loads(r.read().decode('utf-8'))
    print("Catalog items returned:", len(data))
    first = data[0]["file"]
    file_id = str(first["id"]).lstrip('0')
    print("First file ID:", file_id)

# Test play_url
play_url = f"https://store.externulls.com/video/play_url/{file_id}"
req_p = urllib.request.Request(play_url, headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Referer': 'https://beeg.com/',
    'Origin': 'https://beeg.com',
    'Accept': 'application/json, text/plain, */*'
})
try:
    with urllib.request.urlopen(req_p, context=ctx, timeout=10) as r:
        stream_path = r.read().decode('utf-8').strip()
        print(f"play_url response: HTTP {r.status}, body: {stream_path}")
        stream_url = stream_path if stream_path.startswith("http") else f"https://video.beeg.com/{stream_path}"
        print(f"Stream URL: {stream_url}")
        
        # Test probe
        req_stream = urllib.request.Request(stream_url, headers={
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
            'Referer': 'https://beeg.com/',
            'Origin': 'https://beeg.com',
            'Range': 'bytes=0-1024'
        })
        with urllib.request.urlopen(req_stream, context=ctx, timeout=10) as rs:
            print(f"STREAM PROBE SUCCESS: HTTP {rs.status}, Content-Type: {rs.headers.get('Content-Type')}")
except urllib.error.HTTPError as e:
    print(f"play_url failed: HTTP {e.code}, body: {e.read().decode('utf-8', errors='ignore')}")
except Exception as e:
    print(f"Error: {e}")
