with open('tools/series_details_sample.html', 'r', encoding='utf-8') as f:
    html = f.read()

import re
matches = list(re.finditer(r'href=["\'](https://greenmotors\.club/\?id=[^"\']+)["\']', html))
m = matches[0]
start = max(0, m.start() - 1500)
print(html[start:m.start()-300])
