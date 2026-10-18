import urllib.request
import re
import subprocess

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

u = 'https://www.viduki.net/_next/static/chunks/app/1/movie/%5Btmdbid%5D/page-fc682f7b6bcf4f90.js'
req = urllib.request.Request(u, headers={'User-Agent': USER_AGENT, 'Referer': 'https://www.viduki.net/1/movie/550'})
with urllib.request.urlopen(req) as r:
    code = r.read().decode('utf-8', errors='ignore')

# Extract _0x3f4d and _0x541b
m = re.search(r'(function _0x3f4d\(.*function _0x541b\(\)\{let f=\[[^\]]+\];return _0x541b=function\(\)\{return f\},_0x541b\(\)\})', code)
if m:
    node_code = m.group(1) + """
    for (let i = 207; i < 700; i++) {
        try {
            console.log(i + ': ' + _0x3f4d(i));
        } catch (_) {}
    }
    """
    with open("tools/run_decode.js", "w", encoding="utf-8") as f:
        f.write(node_code)
    
    res = subprocess.run(["node", "tools/run_decode.js"], capture_output=True, text=True)
    lines = res.stdout.splitlines()
    print(f"Decoded {len(lines)} strings:")
    for l in lines:
        if any(k in l.lower() for k in ["api", "stream", "server", "http", "main", "vid", "url", "source", "token", "leon", "jill", "claire", "post", "get", "fetch"]):
            print("  ", l)
