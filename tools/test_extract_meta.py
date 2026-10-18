import urllib.request
import json

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Referer': 'https://bingr.one/'
}
req = urllib.request.Request('https://embed.vidrift.in/embed/movie/693134', headers=headers)
with urllib.request.urlopen(req, timeout=10) as r:
    body = r.read().decode('utf-8', errors='ignore')

meta_idx = body.find('embedMeta')
start_brace = body.find('{', meta_idx)
depth = 0
end_brace = -1
in_string = False
escape = False
for i in range(start_brace, len(body)):
    c = body[i]
    if escape:
        escape = False
        continue
    if c == '\\':
        escape = True
        continue
    if c == '"':
        in_string = not in_string
        continue
    if not in_string:
        if c == '{':
            depth += 1
        elif c == '}':
            depth -= 1
            if depth == 0:
                end_brace = i + 1
                break

json_str = body[start_brace:end_brace]
print('Parsed JSON len:', len(json_str))
data = json.loads(json_str)
print('Keys:', list(data.keys()))
orion = data.get('orionStreams') or []
print('Orion count:', len(orion))
for o in orion:
    print('  Stream:', o.get('name'), '| url:', o.get('url'), '| type:', o.get('type'), '| direct:', o.get('direct'))
