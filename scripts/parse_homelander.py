import re

with open('scripts/homelander.html', 'r', encoding='utf-8') as f:
    html = f.read()

elements = re.findall(r'<[a-zA-Z0-9]+[^>]*id=["\'][^"\']+["\'][^>]*>', html)
for el in elements:
    print(el)

print("\n--- Buttons / Links ---")
btns = re.findall(r'<button[^>]*>.*?</button>', html, re.DOTALL)
for b in btns:
    print(b)

links = re.findall(r'<a[^>]*href=["\'][^"\']+["\'][^>]*>.*?</a>', html, re.DOTALL)
for l in links[:20]:
    if not any(x in l for x in ['font', 'icon', 'cdn']):
        print(l[:150])
