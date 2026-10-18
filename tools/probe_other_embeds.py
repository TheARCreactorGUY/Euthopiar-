import urllib.request, ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Referer': 'https://bingr.one/'
}

urls = [
    ("Filmu", "https://embed.filmu.in/movie/550"),
    ("Cinezo", "https://player.cinezo.live/embed/movie/550"),
    ("Vidbolt", "https://vidbolt.xyz/movie/550")
]

for name, u in urls:
    print(f"\n--- Probing {name}: {u} ---")
    try:
        req = urllib.request.Request(u, headers=headers)
        with urllib.request.urlopen(req, context=ctx, timeout=8) as resp:
            body = resp.read().decode('utf-8', errors='ignore')
            with open(f"tools/{name.lower()}_probe.html", "w", encoding="utf-8") as f:
                f.write(body)
            print(f"Status: {resp.status}, length: {len(body)} saved to tools/{name.lower()}_probe.html")
    except Exception as e:
        print(f"Error: {e}")
