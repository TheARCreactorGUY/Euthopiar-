import re

with open('tools/Ads_deobfuscated.js', 'r', encoding='utf-8') as f:
    text = f.read()

idx = text.find('function ue(')
if idx != -1:
    print('Function ue:')
    print(text[idx: idx + 1000])
else:
    # search for ue =
    for m in re.finditer(r'(function\s+ue|const\s+ue\s*=|let\s+ue\s*=)', text):
        print('Found ue declaration:', text[m.start(): m.start() + 500])

# Search for "dcloud"
for m in re.finditer(r'["\']dcloud["\']', text):
    idx = m.start()
    print('Context around "dcloud":')
    print(text[max(0, idx - 200): min(len(text), idx + 200)])
