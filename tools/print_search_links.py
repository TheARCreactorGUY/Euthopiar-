with open('tools/search_sample.html', 'r', encoding='utf-8') as f:
    html = f.read()

import re
matches = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
for h, inner in matches:
    if '4khdhub' in h and not any(k in h for k in ['/category/', '/tag/', '/genre/', 'page']):
        text = re.sub(r'<[^>]+>', ' ', inner).strip()
        img = re.search(r'src=["\']([^"\']+)["\']', inner)
        print(f"HREF: {h}")
        print(f"  TEXT: {text[:60]}")
        if img:
            print(f"  IMG: {img.group(1)}")
        print()
