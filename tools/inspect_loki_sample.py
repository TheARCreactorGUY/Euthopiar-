import re

with open('tools/loki_sample.html', 'r', encoding='utf-8') as f:
    html = f.read()

print(f"Loki sample length: {len(html)}")
file_titles = re.findall(r'<div class="file-title">(.*?)</div>', html, re.DOTALL | re.IGNORECASE)
print(f"Found {len(file_titles)} file-title elements:")
for ft in file_titles:
    clean = re.sub(r'<[^>]+>', '', ft).strip()
    print("  ", clean)

# Check all links
links = re.findall(r'<a[^>]+href=["\'](.*?)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
print(f"Total links: {len(links)}")
for h, t in links[:30]:
    clean_t = re.sub(r'<[^>]+>', '', t).strip()
    if any(k in clean_t.lower() or k in h.lower() for k in ['ep', 'episode', 'download', 'hubcloud', 'greenmotors']):
        print(f"  [{clean_t}] -> {h}")
