import re

with open("tools/Home.js", "r", encoding="utf-8") as f:
    home = f.read()

with open("tools/HeroSlider.js", "r", encoding="utf-8") as f:
    hero = f.read()

print("--- Endpoints in Home.js ---")
endpoints = re.findall(r'["\'](/[^"\']+)["\']', home)
for ep in set(endpoints):
    if any(k in ep for k in ['api', 'trending', 'discover', 'movie', 'tv', 'banner', 'hero', 'carousel', 'home', 'popular']):
        print(" ", ep)

print("--- Endpoints in HeroSlider.js ---")
endpoints = re.findall(r'["\'](/[^"\']+)["\']', hero)
for ep in set(endpoints):
    if any(k in ep for k in ['api', 'trending', 'discover', 'movie', 'tv', 'banner', 'hero', 'carousel', 'home', 'popular']):
        print(" ", ep)

for api_file in ["tools/api-BP3BV6oQ.js", "tools/api-DlNdhc30.js", "tools/api-yyPX6Drj.js"]:
    with open(api_file, "r", encoding="utf-8") as f:
        txt = f.read()
    print(f"--- Endpoints in {api_file} ---")
    eps = re.findall(r'["\'](/[^"\']+)["\']', txt)
    for ep in set(eps):
        if any(k in ep for k in ['api', 'trending', 'discover', 'movie', 'tv', 'banner', 'hero', 'carousel', 'home', 'popular', 'subtitles', 'sources']):
            print(" ", ep)
