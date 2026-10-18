import re

for fname in ["tools/Home.js", "tools/HeroSlider.js", "tools/api-BP3BV6oQ.js", "tools/api-DlNdhc30.js", "tools/api-yyPX6Drj.js"]:
    with open(fname, "r", encoding="utf-8") as f:
        txt = f.read()
    print(f"=== {fname} ===")
    strings = re.findall(r'["\']([^"\']{3,80})["\']', txt)
    filtered = [s for s in strings if any(x in s for x in ['trending', 'discover', 'hero', 'slider', 'banner', 'carousel', 'home', 'movie', 'anime', 'subtitles', 'sources', 'embed', 'player', 'vidrift', 'vidy', 'direct', 'stream'])]
    print("Matches:", len(filtered))
    for s in filtered[:25]:
        print(" ", s)
