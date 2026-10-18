import requests, re

js = requests.get('https://player.vidzee.wtf/assets/index-BNFRNzEx.js').text
print('JS len:', len(js))
fetches = re.findall(r'fetch\([^\)]+\)', js)
print('Fetches:', fetches[:10])

# Check string literals containing /
paths = set(re.findall(r'"(/[a-zA-Z0-9_\-/]+)"', js))
print('Paths:', [p for p in paths if not p.startswith('/assets')][:15])
