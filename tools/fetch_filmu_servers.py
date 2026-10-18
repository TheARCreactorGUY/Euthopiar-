import urllib.request, re

headers = {'User-Agent': 'Mozilla/5.0', 'Referer': 'https://embed.filmu.in/'}
req = urllib.request.Request('https://embed.filmu.in/assets/servers-B7U1aDDl.js', headers=headers)
try:
    with urllib.request.urlopen(req) as r:
        txt = r.read().decode('utf-8')
        print("servers-B7U1aDDl.js length:", len(txt))
        print("URLs in file:")
        for u in set(re.findall(r'https?://[^\s"\'`]+', txt)):
            print("  ", u)
        with open("tools/filmu_servers.js", "w", encoding="utf-8") as f:
            f.write(txt)
except Exception as e:
    print("Error:", e)
