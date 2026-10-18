import urllib.request
import re
import json

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
}

req = urllib.request.Request('https://bingr.one/home', headers=headers)
with urllib.request.urlopen(req) as resp:
    html = resp.read().decode('utf-8')

print("Page HTML length:", len(html))
scripts = re.findall(r'src=["\']([^"\']+\.js[^"\']*)["\']', html)
print("Scripts found in HTML:")
for s in scripts:
    print(" ", s)

idx_url = "https://bingr.one/assets/index-DL3V52UJ.js"
req = urllib.request.Request(idx_url, headers=headers)
with urllib.request.urlopen(req) as resp:
    idx_js = resp.read().decode('utf-8')

print("Index JS length:", len(idx_js))
chunks = re.findall(r'["\'](/assets/[^"\']+\.js)["\']', idx_js)
print("Chunks in index:")
for c in sorted(set(chunks)):
    print(" ", c)

# Search for any other chunks mentioned
all_chunks = re.findall(r'([a-zA-Z0-9_\-]+-[a-zA-Z0-9_\-]+\.js)', idx_js)
print("All JS references in index:")
for c in sorted(set(all_chunks)):
    print(" ", c)

