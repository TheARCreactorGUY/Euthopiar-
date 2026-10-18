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

targets = ["vidfast", "vidlink", "db.1shows.org", "servers", "provider", "player"]

for c in chunks:
    chunk_code = get(f"https://www.1shows.org/_next/static/chunks/{c}")
    for t in ["vidfast", "vidlink", "db.1shows.org", "viduki"]:
        if t in chunk_code:
            print(f"=== Found '{t}' in chunk {c} ===")
            for m in re.finditer(re.escape(t), chunk_code):
                start = max(0, m.start() - 200)
                end = min(len(chunk_code), m.end() + 200)
                snippet = chunk_code[start:end].replace('\n', ' ')
                print(f"[{t}]: ... {snippet} ...\n")
