import urllib.request
import ssl

ctx = ssl._create_unverified_context()
url = "https://s32.bigcdn.cc/pubs/6ac4cf3cb39386.70865805/1080.mp4"
req = urllib.request.Request(url, headers={
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Referer': 'https://mydaddy.cc/',
    'Range': 'bytes=0-1024'
})
try:
    with urllib.request.urlopen(req, context=ctx, timeout=10) as r:
        print("HQPORNER DIRECT STREAM PROBE:", r.status, r.headers.get("Content-Type"), "Content-Length:", r.headers.get("Content-Length"))
except Exception as e:
    print("Direct stream probe failed:", e)
