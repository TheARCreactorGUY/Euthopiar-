import urllib.request
import ssl

ctx = ssl._create_unverified_context()

eporner_tlds = [
    "www.eporner.org",
    "www.eporner.me",
    "www.eporner.cc",
    "www.eporner.xxx",
    "www.eporner.link",
    "www.eporner.io",
    "www.eporner.to",
    "www.eporner.is",
    "www.eporner.club",
    "www.eporner.co"
]

for m in eporner_tlds:
    url = f"https://{m}/"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=4) as r:
            print(f"Eporner mirror {m:20}: OK -> {r.status}")
    except Exception as e:
        print(f"Eporner mirror {m:20}: FAIL -> {e}")
