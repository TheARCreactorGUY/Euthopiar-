import re

with open('tools/hubcloud_sample.html', 'r', encoding='utf-8') as f:
    html = f.read()

print(f"Hubcloud sample length: {len(html)}")
title = re.search(r'<title>(.*?)</title>', html)
if title: print("Title:", title.group(1))

links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
print(f"Found {len(links)} links:")
for h, t in links:
    clean = re.sub(r'<[^>]+>', ' ', t).strip()
    print(f"  [{clean}] -> {h}")
