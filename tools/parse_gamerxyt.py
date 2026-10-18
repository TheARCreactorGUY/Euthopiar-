import re

with open('tools/gamerxyt_sample.html', 'r', encoding='utf-8') as f:
    html = f.read()

links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL | re.IGNORECASE)
for h, t in links:
    clean_t = re.sub(r'<[^>]+>', '', t).strip()
    clean_t = re.sub(r'\s+', ' ', clean_t)
    # Filter out advertising or navigation
    if any(k in h for k in ['telegram', 'tinyurl', 'one.one.one.one', 'a-ads', 'winexch', 'HDhub4u', 'admin']):
        continue
    print(f"[{clean_t}] -> {h}")
