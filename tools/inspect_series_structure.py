with open('tools/series_details_sample.html', 'r', encoding='utf-8') as f:
    html = f.read()

import re
matches = re.findall(r'<div class="file-title">(.*?)</div>', html, re.DOTALL | re.IGNORECASE)
print(f"Found {len(matches)} .file-title elements:")
for m in matches[:10]:
    clean = re.sub(r'<[^>]+>', '', m).strip()
    print(f"  Title: {clean}")

# Also check episode labels or season buttons
ep_labels = re.findall(r'<(?:button|span|h3|h4)[^>]*>(Episode\s*\d+|Season\s*\d+|S\d+E\d+|Zip\s*Pack.*?)</(?:button|span|h3|h4)>', html, re.IGNORECASE)
print(f"Found {len(ep_labels)} episode/season labels:")
for l in ep_labels[:15]:
    print(f"  Label: {l}")
