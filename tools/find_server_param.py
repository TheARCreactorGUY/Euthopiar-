import re

with open('tools/Ads_deobfuscated.js', 'r', encoding='utf-8') as f:
    text = f.read()

# Search for calls to ne(
for m in re.finditer(r'ne\([^)]+\)', text):
    print('Call to ne:', m.group(0))

# Search for 's=' or 'server'
for m in re.finditer(r'[\'"][^\'"]*server[^\'"]*[\'"]', text):
    print('Server string:', m.group(0))

# Search for the UI server list or dropdown
for m in re.finditer(r'servers|serverList|serverOptions', text, re.IGNORECASE):
    idx = m.start()
    print('Context around servers keyword:')
    print(text[max(0, idx - 100): min(len(text), idx + 200)])
    print('-' * 40)
