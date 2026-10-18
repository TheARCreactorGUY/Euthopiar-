import urllib.request, re, json

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'}

def fetch(url):
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req) as resp:
        return resp.read().decode('utf-8')

print("Fetching Home...")
home_js = fetch("https://bingr.one/assets/Home-C_mNuTe3.js")
print("Home length:", len(home_js))
with open("tools/Home.js", "w", encoding="utf-8") as f:
    f.write(home_js)

print("Fetching HeroSlider...")
hero_js = fetch("https://bingr.one/assets/HeroSlider-DLGKldXJ.js")
print("HeroSlider length:", len(hero_js))
with open("tools/HeroSlider.js", "w", encoding="utf-8") as f:
    f.write(hero_js)

print("Fetching Watch...")
watch_js = fetch("https://bingr.one/assets/Watch-BNatmtrN.js")
print("Watch length:", len(watch_js))
with open("tools/Watch.js", "w", encoding="utf-8") as f:
    f.write(watch_js)

for a in ["api-BP3BV6oQ.js", "api-DlNdhc30.js", "api-yyPX6Drj.js"]:
    print(f"Fetching {a}...")
    txt = fetch(f"https://bingr.one/assets/{a}")
    with open(f"tools/{a}", "w", encoding="utf-8") as f:
        f.write(txt)
