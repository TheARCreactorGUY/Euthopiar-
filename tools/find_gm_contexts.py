with open('tools/series_details_sample.html', 'r', encoding='utf-8') as f:
    html = f.read()

import re
# Find all divs or elements containing greenmotors links
matches = re.finditer(r'href=["\'](https://greenmotors\.club/\?id=[^"\']+)["\']', html)
contexts = []
for m in list(matches)[:15]:
    start = max(0, m.start() - 200)
    end = min(len(html), m.end() + 100)
    contexts.append(html[start:end])

for i, c in enumerate(contexts[:5]):
    print(f"--- Context {i+1} ---")
    print(c)
