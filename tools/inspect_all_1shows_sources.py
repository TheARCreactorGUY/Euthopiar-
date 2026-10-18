import urllib.request
import json
import re

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

def get(url, referer="https://www.1shows.org/"):
    req = urllib.request.Request(url, headers={
        "User-Agent": USER_AGENT,
        "Referer": referer,
        "Origin": "https://www.1shows.org"
    })
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.status, r.read().decode('utf-8', errors='ignore')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', errors='ignore')
    except Exception as e:
        return 0, str(e)

print("--- 1. Inspecting 1shows.org Movie 550 HTML ---")
st, html = get("https://www.1shows.org/movies/550")
print(f"Status: {st}, Length: {len(html)}")

# Find next build id and all script chunks
build_id = re.findall(r'"buildId":"([^"]+)"', html)
print(f"Build ID: {build_id}")

chunks = re.findall(r'/_next/static/chunks/([^"]+\.js)', html)
print(f"Found {len(chunks)} JS chunks:")
for c in chunks[:15]:
    print(f"  {c}")

# Let's search inside the JS chunks for server endpoints, providers, and embed URLs
print("\n--- 2. Scanning JS chunks for servers, providers, APIs ---")
server_mentions = set()
api_mentions = set()
for c in chunks:
    chunk_url = f"https://www.1shows.org/_next/static/chunks/{c}"
    _, chunk_code = get(chunk_url)
    
    # Look for domains, APIs, endpoints, server names
    domains = re.findall(r'https?://[a-zA-Z0-9\.\-_]+\.[a-zA-Z]{2,}(?:/[a-zA-Z0-9_\-\.\?=&%]*)?', chunk_code)
    for d in domains:
        if any(k in d for k in ["api", "stream", "player", "embed", "vid", "show", "watch", "server", "cdn", "source"]):
            api_mentions.add(d)

    # Look for server lists / object keys
    servers = re.findall(r'["\']([a-zA-Z0-9_-]+)["\']:\s*\{\s*["\'](?:name|url|id|type|endpoint)["\']', chunk_code)
    server_mentions.update(servers)

print(f"\nDiscovered API / Domain references ({len(api_mentions)}):")
for a in sorted(api_mentions):
    print(f"  {a}")

print(f"\nDiscovered Server object keys ({len(server_mentions)}):")
for s in sorted(server_mentions):
    print(f"  {s}")

