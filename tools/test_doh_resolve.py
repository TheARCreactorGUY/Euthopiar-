import urllib.request
import json
import ssl

ctx = ssl._create_unverified_context()

def doh_resolve(domain):
    url = f"https://cloudflare-dns.com/dns-query?name={domain}&type=A"
    req = urllib.request.Request(url, headers={"Accept": "application/dns-json"})
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=5) as r:
            data = json.loads(r.read().decode('utf-8'))
            ips = [ans["data"] for ans in data.get("Answer", []) if ans.get("type") == 1]
            return ips
    except Exception as e:
        return f"Error: {e}"

for d in ["www.eporner.com", "eporner.com", "www.xvideos.com", "xvideos.com"]:
    print(d, "->", doh_resolve(d))
