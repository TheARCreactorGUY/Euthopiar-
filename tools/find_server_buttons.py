import urllib.request
import re

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

req = urllib.request.Request("https://www.1shows.org/_next/static/chunks/app/layout-b71aea8620f25560.js", headers={"User-Agent": USER_AGENT, "Referer": "https://www.1shows.org/"})
with urllib.request.urlopen(req) as r:
    code = r.read().decode('utf-8', errors='ignore')

# Search for P(e.id) or where server buttons are rendered
pos = code.find("P(e.id)")
if pos == -1:
    pos = code.find("1shows-server")
print("pos:", pos)
print(code[max(0, pos-1000):min(len(code), pos+2000)])
