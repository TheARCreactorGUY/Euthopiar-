with open('tools/series_details_sample.html', 'r', encoding='utf-8') as f:
    html = f.read()

import re
matches = list(re.finditer(r'href=["\'](https://greenmotors\.club/\?id=[^"\']+)["\']', html))
for i in [0, 2, 4]:
    m = matches[i]
    start = max(0, m.start() - 600)
    print(f"=== BEFORE LINK {i+1} ===")
    print(html[start:m.start()])
