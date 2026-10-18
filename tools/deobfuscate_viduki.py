import urllib.request
import re

USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

u = 'https://www.viduki.net/_next/static/chunks/app/1/movie/%5Btmdbid%5D/page-fc682f7b6bcf4f90.js'
req = urllib.request.Request(u, headers={'User-Agent': USER_AGENT, 'Referer': 'https://www.viduki.net/1/movie/550'})
with urllib.request.urlopen(req) as r:
    code = r.read().decode('utf-8', errors='ignore')

# Look for _0x3f4d string array
pos = code.find("function _0x")
if pos == -1:
    pos = code.find("_0x")
print("Pos of _0x:", pos)
print(code[pos:pos+1500])
