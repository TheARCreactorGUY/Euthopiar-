import urllib.request, urllib.parse, re

base_url = "https://4khdhub.one"
headers = {'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36'}

for q in ["Loki", "Stranger Things", "The Boys"]:
    print(f"\n================ Search: {q} ================")
    u = f"{base_url}/?s={urllib.parse.quote(q)}"
    req = urllib.request.Request(u, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            html = resp.read().decode('utf-8', errors='ignore')
    except Exception as e:
        print(f"Error searching {q}: {e}")
        continue
    
    # Extract links
    cards = re.findall(r'<a[^>]+href=["\'](https://4khdhub\.[^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
    seen = set()
    for link, body in cards:
        if link in seen or 'page/' in link: continue
        seen.add(link)
        title = re.sub(r'<[^>]+>', '', body).strip().encode('ascii', 'ignore').decode('ascii')
        if len(title) > 5:
            print(f"Card: {title[:80]} -> {link}")
