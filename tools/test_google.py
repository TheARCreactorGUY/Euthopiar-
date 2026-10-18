import urllib.request
import urllib.parse
import time

def test_google_direct():
    user_agent = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
    
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
        
    print("Full Redirect Location:", loc)
    
    # 1. Test the inner googleusercontent link
    inner = loc.split('url=')[1].split('&')[0]
    inner_decoded = urllib.parse.unquote(inner)
    print("\n[Test 1] Direct googleusercontent link:")
    print(inner_decoded[:100] + "...")
    
    # Check headers without range first
    req_head = urllib.request.Request(inner_decoded, headers={'User-Agent': user_agent})
    try:
        with urllib.request.urlopen(req_head, timeout=10) as h_resp:
            print(f"Status: {h_resp.status}")
            print(f"Content-Length: {h_resp.headers.get('Content-Length')}")
            print(f"Content-Type: {h_resp.headers.get('Content-Type')}")
            print(f"Accept-Ranges: {h_resp.headers.get('Accept-Ranges')}")
            # Read 2MB
            t0 = time.time()
            chunk = h_resp.read(2 * 1024 * 1024)
            dt = time.time() - t0
            print(f"Read 2MB in {dt:.2f}s ({len(chunk)/1024/1024*8/dt:.2f} Mbps)")
    except Exception as e:
        print("Error on direct googleusercontent:", e)
        
    # Check with Range: bytes=0-2097151 (2MB)
    print("\n[Test 2] Direct googleusercontent WITH Range: bytes=0-2097151")
    req_range = urllib.request.Request(inner_decoded, headers={'User-Agent': user_agent, 'Range': 'bytes=0-2097151'})
    try:
        with urllib.request.urlopen(req_range, timeout=10) as r_resp:
            print(f"Status: {r_resp.status}")
            print(f"Content-Range: {r_resp.headers.get('Content-Range')}")
            t0 = time.time()
            chunk = r_resp.read()
            dt = time.time() - t0
            print(f"Read {len(chunk)/1024/1024:.2f} MB in {dt:.2f}s ({len(chunk)/1024/1024*8/dt:.2f} Mbps)")
    except Exception as e:
        print("Error on range request:", e)

    # 3. Test the proxy link (video-seed.dev)
    print("\n[Test 3] video-seed.dev link:")
    req_proxy = urllib.request.Request(loc, headers={'User-Agent': user_agent})
    try:
        with urllib.request.urlopen(req_proxy, timeout=10) as p_resp:
            print(f"Status: {p_resp.status}")
            print(f"Content-Length: {p_resp.headers.get('Content-Length')}")
            print(f"Content-Range: {p_resp.headers.get('Content-Range')}")
            t0 = time.time()
            chunk = p_resp.read(2 * 1024 * 1024)
            dt = time.time() - t0
            print(f"Read 2MB in {dt:.2f}s ({len(chunk)/1024/1024*8/dt:.2f} Mbps)")
    except Exception as e:
        print("Error on proxy link:", e)

if __name__ == '__main__':
    test_google_direct()
