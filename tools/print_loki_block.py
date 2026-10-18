with open('tools/loki_sample.html', 'r', encoding='utf-8') as f:
    html = f.read()

import re
m = re.search(r'<div class="file-title">Loki S01 1080p BluRay REMUX.*?</div>(.*?)<div class="file-title">', html, re.DOTALL)
if m:
    block = m.group(1)
    print("Block length:", len(block))
    links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', block, re.DOTALL)
    print(f"Found {len(links)} links in block:")
    for h, t in links:
        clean = re.sub(r'<[^>]+>', ' ', t).strip()
        print(f"  [{clean}] -> {h}")
