import re

with open('scripts/homelander.html', 'r', encoding='utf-8') as f:
    html = f.read()

scripts = re.findall(r'<script[^>]*>(.*?)</script>', html, re.DOTALL | re.IGNORECASE)
for i, s in enumerate(scripts):
    if '_0x4c76' in s:
        with open('scripts/script8.js', 'w', encoding='utf-8') as sf:
            sf.write(s)
        print(f"Saved script {i} to scripts/script8.js (length {len(s)})")
