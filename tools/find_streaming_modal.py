import urllib.request
import re

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

req = urllib.request.Request("https://www.1shows.org/_next/static/chunks/app/layout-b71aea8620f25560.js", headers={"User-Agent": USER_AGENT, "Referer": "https://www.1shows.org/"})
with urllib.request.urlopen(req) as r:
    code = r.read().decode('utf-8', errors='ignore')

# Search for iframe src or server buttons
pos = code.find("streaming")
while pos != -1:
    snippet = code[pos:pos+1500]
    if "iframe" in snippet or "src:" in snippet or "MAIN" in snippet or "server" in snippet:
        print(f"=== Found at {pos} ===")
        print(snippet[:600])
        print("...\n")
    pos = code.find("streaming", pos+1)
    if pos > 60000: break
