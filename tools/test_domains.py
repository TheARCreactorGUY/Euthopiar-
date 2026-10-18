import urllib.request
import urllib.error

DOMAINS = [
    "https://www.1shows.bz",
    "https://1shows.bz",
    "https://www.1shows.cx",
    "https://1shows.cx",
    "https://api.viduki.net/download-token",
    "https://api.viduki.net/main/servers",
    "https://player.vidzee.wtf",
    "https://core.vidzee.wtf",
    "https://vidrock.net"
]

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.9",
    "Referer": "https://www.google.com/"
}

for d in DOMAINS:
    req = urllib.request.Request(d, headers=HEADERS)
    try:
        with urllib.request.urlopen(req, timeout=7) as resp:
            print(f"[{resp.status}] {d}")
    except urllib.error.HTTPError as e:
        print(f"[{e.code}] {d}")
    except Exception as e:
        print(f"[ERR] {d} -> {e}")
