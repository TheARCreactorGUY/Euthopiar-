import urllib.request
import time

def test_stream_speed():
    user_agent = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
    
    # 1. Resolve CDN link
    cdn_url = 'https://cdn.video-gen.xyz/7e12da5daf868802721f034f904a0f4f61958c8e63cf32b46045f571622c8d7d82ecb9ce683269ac36f9761fd9565b034fc4ee4efce8268a082ca3db0e9ebd04ff892b96f1d506ad1b7fb3ffe391f4a7c65c2122a5e35020ba351176e5dc06309fb9ec13b7ede33a2638a1a0e5cba125::57b9e0b061835cc922d06a49ad06e249'
    
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, req, fp, code, msg, headers, newurl):
            return None

    opener = urllib.request.build_opener(NoRedirect)
    req = urllib.request.Request(cdn_url, headers={'User-Agent': user_agent, 'Referer': 'https://driveseed.org/'})
    
    try:
        resp = opener.open(req)
        loc = resp.headers.get('Location')
    except urllib.error.HTTPError as e:
        loc = e.headers.get('Location')
        
    print("Redirect Location:", loc[:120] if loc else "None")
    if not loc:
        return
        
    # Test streaming chunks from this URL
    stream_url = loc
    headers = {
        'User-Agent': user_agent,
        'Referer': 'https://driveseed.org/',
        'Range': 'bytes=0-10485760' # 10MB chunk
    }
    
    print("\nRequesting 10MB chunk...")
    t0 = time.time()
    req_stream = urllib.request.Request(stream_url, headers=headers)
    with urllib.request.urlopen(req_stream, timeout=10) as s_resp:
        data = s_resp.read()
        t1 = time.time()
        size_mb = len(data) / (1024 * 1024)
        duration = t1 - t0
        speed_mbps = (size_mb * 8) / duration if duration > 0 else 0
        print(f"Downloaded {size_mb:.2f} MB in {duration:.2f}s ({speed_mbps:.2f} Mbps) - HTTP {s_resp.status}")
        
    # Test second chunk (like seeking or continuous buffering in ExoPlayer)
    headers2 = {
        'User-Agent': user_agent,
        'Referer': 'https://driveseed.org/',
        'Range': 'bytes=10485761-20971520' # Next 10MB
    }
    print("\nRequesting next 10MB chunk...")
    t0 = time.time()
    req_stream2 = urllib.request.Request(stream_url, headers=headers2)
    with urllib.request.urlopen(req_stream2, timeout=10) as s_resp:
        data2 = s_resp.read()
        t1 = time.time()
        size_mb = len(data2) / (1024 * 1024)
        duration = t1 - t0
        speed_mbps = (size_mb * 8) / duration if duration > 0 else 0
        print(f"Downloaded {size_mb:.2f} MB in {duration:.2f}s ({speed_mbps:.2f} Mbps) - HTTP {s_resp.status}")

if __name__ == '__main__':
    test_stream_speed()
