with open('tools/movie_details_sample.html', 'r', encoding='utf-8') as f:
    html = f.read()

import re
# Find all download boxes
# Search for download-item, download-header, file-title, etc.
boxes = re.findall(r'<div[^>]*class=["\'][^"\']*(?:download-box|download-item|file-item|link-item)[^"\']*["\'][^>]*>(.*?)</div>\s*</div>', html, re.DOTALL | re.IGNORECASE)
print(f"Found {len(boxes)} boxes via class")

# Search for h3 or h4 headers with resolution (4K, 1080p, 720p, etc.)
headers = re.findall(r'<(?:h3|h4)[^>]*>(.*?)</(?:h3|h4)>', html, re.DOTALL | re.IGNORECASE)
print(f"Found {len(headers)} h3/h4 headers:")
for h in headers[:10]:
    clean = re.sub(r'<[^>]+>', '', h).strip()
    if any(q in clean.lower() for q in ['4k', '2160p', '1080p', '720p', '480p', 'download']):
        print(f"  Header: {clean}")

# Find greenmotors or cloud links
links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)
dl_links = []
for h, t in links:
    if any(k in h for k in ['greenmotors', 'hubcloud', 'hubdrive', 'drive', 'download', 'fastdl']):
        clean_t = re.sub(r'<[^>]+>', '', t).strip()
        dl_links.append((clean_t, h))

print(f"\nFound {len(dl_links)} download links:")
for t, h in dl_links[:15]:
    print(f"  [{t}] -> {h}")
