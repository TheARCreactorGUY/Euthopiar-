import re

with open('tools/series_details_sample.html', 'r', encoding='utf-8') as f:
    html = f.read()

parts = html.split('class="file-title"')
for i, p in enumerate(parts[1:4]):
    print(f"=== Part {i} ===")
    chunk = p[:2000]
    hrefs = re.findall(r'<a[^>]+href=["\'](.*?)["\'][^>]*>(.*?)</a>', chunk, re.DOTALL)
    for h, txt in hrefs:
        clean_txt = re.sub(r'<[^>]+>', '', txt).strip()
        print(f"   href: {h} | text: {clean_txt}")
