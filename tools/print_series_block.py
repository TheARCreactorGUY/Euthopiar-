with open('tools/series_details_sample.html', 'r', encoding='utf-8') as f:
    html = f.read()

import re
# Find content of first file-title block
m = re.search(r'<div class="file-title">Lanterns\.S01\.2160p.*?</div>(.*?)<div class="file-title">', html, re.DOTALL)
if m:
    block = m.group(1)
    print("Block length:", len(block))
    # Find all links and their text
    links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', block, re.DOTALL)
    print(f"Found {len(links)} links in block:")
    for h, t in links[:20]:
        clean = re.sub(r'<[^>]+>', ' ', t).strip()
        print(f"  [{clean}] -> {h[:60]}...")
