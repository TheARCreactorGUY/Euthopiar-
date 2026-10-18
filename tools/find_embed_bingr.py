import urllib.request

req = urllib.request.Request("https://embed.bingr.one/embed/movie/550", headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req) as resp:
        html = resp.read().decode('utf-8')
        print("embed.bingr.one movie/550 status 200, length:", len(html))
        with open("tools/embed_bingr.html", "w", encoding="utf-8") as f:
            f.write(html)
except Exception as e:
    print("embed.bingr.one error:", e)
