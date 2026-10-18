import urllib.request
import re

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT, "Referer": "https://www.1shows.org/"})
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.read().decode('utf-8', errors='ignore')
    except Exception as e:
        return ""

html = get("https://www.1shows.org/movies/550")
chunks = re.findall(r'/_next/static/chunks/([^"]+\.js)', html)

for c in chunks:
    code = get(f"https://www.1shows.org/_next/static/chunks/{c}")
    if "MAIN_1" in code:
        print(f"=== Chunk {c} has MAIN_1 ===")
        pos = code.find("MAIN_1")
        while pos != -1:
            print(code[max(0, pos-400):min(len(code), pos+600)])
            print("-" * 50)
            pos = code.find("MAIN_1", pos+1)
