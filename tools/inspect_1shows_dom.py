import urllib.request
import re

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Referer": "https://www.google.com/"
}

url = "https://www.1shows.bz/"
req = urllib.request.Request(url, headers=HEADERS)
try:
    with urllib.request.urlopen(req, timeout=10) as resp:
        html = resp.read().decode('utf-8', errors='replace')
        print(f"Loaded {url}: {len(html)} bytes")
        # Find all script src
        scripts = re.findall(r'<script[^>]+src=["\']([^"\']+)["\']', html)
        print("Scripts:")
        for s in scripts:
            print(f"  {s}")
        # Find any embedded iframes or API calls
        iframes = re.findall(r'<iframe[^>]+src=["\']([^"\']+)["\']', html)
        print("Iframes:")
        for i in iframes:
            print(f"  {i}")
        # Find URLs in inline scripts
        api_urls = set(re.findall(r'https?://[a-zA-Z0-9\.\-_]+\.[a-zA-Z]{2,}[^\s"\'<>]*', html))
        print("API / External URLs found:")
        for u in sorted(api_urls):
            if any(k in u for k in ["vid", "show", "stream", "api", "embed", "player"]):
                print(f"  {u}")
except Exception as e:
    print(f"Error: {e}")
