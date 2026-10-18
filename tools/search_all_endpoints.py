import re

for fname in ["tools/Home.js", "tools/HeroSlider.js", "tools/Watch.js"]:
    with open(fname, "r", encoding="utf-8") as f:
        txt = f.read()
    print(f"\n==================== {fname} ====================")
    # Search for anything looking like a path or URL
    paths = re.findall(r'[`"\'/](/[a-zA-Z0-9_\-\./\?\=\&\{\}\$]+)[`"\']', txt)
    endpoints = [p for p in set(paths) if any(x in p for x in ['api', 'trending', 'discover', 'hero', 'slider', 'movie', 'tv', 'anime', 'subtitles', 'sources', 'embed', 'stream', 'carousel', 'banner'])]
    for ep in sorted(endpoints):
        print("  EP:", ep)
