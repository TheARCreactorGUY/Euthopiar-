import urllib.request
import subprocess

u = 'https://www.viduki.net/_next/static/chunks/app/1/movie/%5Btmdbid%5D/page-fc682f7b6bcf4f90.js'
req = urllib.request.Request(u, headers={'User-Agent': 'Mozilla/5.0', 'Referer': 'https://www.viduki.net/1/movie/550'})
with urllib.request.urlopen(req) as r:
    code = r.read().decode('utf-8', errors='ignore')

idx = code.find(')(_0x541b,0)') + len(')(_0x541b,0)')
node_script = code[:idx] + """;
const results = {};
for (let i = 207; i < 650; i++) {
    try {
        results[i] = _0x3f4d(i);
    } catch (_) {}
}
console.log(JSON.stringify(results, null, 2));
"""

with open("tools/run_deobf.js", "w", encoding="utf-8") as f:
    f.write(node_script)

res = subprocess.run(["node", "tools/run_deobf.js"], capture_output=True, text=True)
with open("tools/deobf_strings.json", "w", encoding="utf-8") as f:
    f.write(res.stdout)
print("Deobfuscated", len(res.stdout), "bytes of strings.")
