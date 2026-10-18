import urllib.request

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36', 'Referer': 'https://bingr.one/'}

for name, url in [
    ('filmu', 'https://embed.filmu.in/movie/550'),
    ('cinezo', 'https://player.cinezo.live/embed/movie/550'),
    ('vidbolt', 'https://vidbolt.xyz/movie/550')
]:
    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=8) as r:
            content = r.read().decode('utf-8', errors='ignore')
            with open(f"tools/{name}_page.html", "w", encoding="utf-8") as f:
                f.write(content)
            print(f"Saved {name}_page.html: {len(content)} bytes")
    except Exception as e:
        print(f"Error {name}: {e}")
