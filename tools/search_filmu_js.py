import urllib.request, re

headers = {'User-Agent': 'Mozilla/5.0'}
req = urllib.request.Request('https://embed.filmu.in/assets/index-Ciw5QmJy.js', headers=headers)
with urllib.request.urlopen(req) as r:
    txt = r.read().decode('utf-8')
    print("Filmu index.js length:", len(txt))
    urls = set(re.findall(r'https?://[^\s"\'`]+', txt))
    for u in sorted(urls):
        if any(k in u for k in ['filmu', 'api', 'source', 'stream', 'm3u8']):
            print("  url:", u)

    # Search for template strings like `api/` or `sources`
    templates = set(re.findall(r'[`"\'][^`"\']*(?:api|source|stream|servers)[^`"\']*[`"\']', txt))
    print("\nTemplates:")
    for t in list(templates)[:30]:
        print("  tmpl:", t)
