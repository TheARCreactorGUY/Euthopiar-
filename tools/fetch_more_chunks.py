import urllib.request, re, json

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'}

def fetch(url):
    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req) as resp:
            return resp.read().decode('utf-8')
    except Exception as e:
        print(f"Error fetching {url}: {e}")
        return ""

chunks_to_fetch = [
    "Movies-CKItSs1Y.js",
    "Tv-BnhBcgGi.js",
    "Anime-CiDyWT4t.js",
    "MovieDetails-FmCUyFJl.js",
    "TvDetails-CKqgoTJP.js",
    "AnimeDetails-xGGA17NG.js",
    "Categories-C6KmKM4r.js",
    "CategoryDetail-moVSBxnd.js",
    "ContentSections-DVGU_g2Q.js",
    "EmbedLanding-Dyzt7HOW.js",
    "DownloadApp-DmG40ILg.js"
]

for c in chunks_to_fetch:
    txt = fetch(f"https://bingr.one/assets/{c}")
    if txt:
        with open(f"tools/{c}", "w", encoding="utf-8") as f:
            f.write(txt)
        print(f"Fetched {c}: {len(txt)} bytes")
