import urllib.request
import ssl

ctx = ssl._create_unverified_context()

def check_domain(domain):
    url = f"https://{domain}/"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=5) as r:
            return True, r.status
    except Exception as e:
        return False, str(e)

xvideos_mirrors = [
    "www.xvideos.com",
    "www.xvideos2.com",
    "www.xvideos3.com",
    "www.xvideos.es",
    "www.xvideos.red",
    "xvideos.com",
    "xvideos2.com",
    "xvideos3.com"
]

for m in xvideos_mirrors:
    ok, res = check_domain(m)
    print(f"Xvideos mirror {m:20}: {'OK' if ok else 'FAIL'} -> {res}")

eporner_mirrors = [
    "www.eporner.com",
    "eporner.com",
    "www.eporner.tv",
    "eporner.tv",
    "www.eporner.net",
    "eporner.net"
]

for m in eporner_mirrors:
    ok, res = check_domain(m)
    print(f"Eporner mirror {m:20}: {'OK' if ok else 'FAIL'} -> {res}")
