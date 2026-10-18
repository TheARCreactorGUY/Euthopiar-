import urllib.request
import ssl
import json
import socket

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

def doh_lookup(hostname):
    req = urllib.request.Request(
        f"https://cloudflare-dns.com/dns-query?name={hostname}&type=A",
        headers={"Accept": "application/dns-json"}
    )
    with urllib.request.urlopen(req, context=ctx) as r:
        data = json.loads(r.read().decode())
        answers = data.get("Answer", [])
        ips = [a["data"] for a in answers if a["type"] == 1]
        return ips

print("DoH lookup for www.viduki.net:", doh_lookup("www.viduki.net"))
print("DoH lookup for api.viduki.net:", doh_lookup("api.viduki.net"))
