import urllib.request
import ssl
import re

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
headers = {'User-Agent': 'Mozilla/5.0'}

req = urllib.request.Request('https://cinejoy.pk/_app/immutable/entry/app.BG_KW6jq.js', headers=headers)
with urllib.request.urlopen(req, context=ctx) as r:
    app_js = r.read().decode('utf-8', errors='ignore')

# Find node imports
node_files = re.findall(r'nodes/([a-zA-Z0-9_\-\.]+)', app_js)
print('Node files in app.js:', node_files)

for nf in set(node_files):
    u = f'https://cinejoy.pk/_app/immutable/nodes/{nf}'
    try:
        r2 = urllib.request.Request(u, headers=headers)
        with urllib.request.urlopen(r2, context=ctx) as resp:
            js = resp.read().decode('utf-8', errors='ignore')
        urls = set(re.findall(r'https?://[a-zA-Z0-9\.\-_/]+', js))
        clean_urls = [x for x in urls if not 'svelte.dev' in x and not 'w3.org' in x]
        if clean_urls:
            print(f'Node {nf} URLs:', clean_urls)
        # Search for servers or endpoints
        servers = set(re.findall(r'["\'](Helios|Lisbon|Nebula|Nova|Titan|Moscow|Berlin|Tokyo|Paris|Granite|Wing|Natsuki|Alpha|Omega)["\']', js, re.I))
        if servers:
            print(f'Node {nf} servers:', servers)
    except Exception as e:
        print(f'Error fetching {nf}: {e}')
