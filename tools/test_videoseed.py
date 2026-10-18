import urllib.request
import urllib.parse
import time

def test_videoseed():
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
        
    print("Full Location:", loc[:100] + "...")
    
    # Test requesting `loc` (which is video-seed.dev/?url=...) with different Range headers
    headers_range = {
        'User-Agent': user_agent,
        'Referer': 'https://driveseed.org/',
        'Range': 'bytes=0-1048576'
    }
    
    req_seed = urllib.request.Request(loc, headers=headers_range)
    try:
        with urllib.request.urlopen(req_seed, timeout=10) as resp:
            print("Status:", resp.status)
            for k, v in resp.headers.items():
                if any(x in k.lower() for x in ['content', 'range', 'accept', 'transfer']):
                    print(f"  {k}: {v}")
            data = resp.read()
            print(f"Read: {len(data)} bytes")
    except Exception as e:
        print("Error on video-seed.dev range:", e)

if __name__ == '__main__':
    test_videoseed()
