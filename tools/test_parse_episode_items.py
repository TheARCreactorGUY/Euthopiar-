import urllib.request, re

url = "https://4khdhub.one/marvels-the-punisher-series-2288/"
headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
req = urllib.request.Request(url, headers=headers)
with urllib.request.urlopen(req, timeout=12) as resp:
    html = resp.read().decode('utf-8', errors='ignore')

# Split or select episode-download-item
items = html.split('class="episode-download-item"')
print(f"Found {len(items)-1} episode-download-item blocks!")

ep_data = []
for it in items[1:]:
    chunk = it[:2500]
    title_m = re.search(r'class="episode-file-title">\s*(.*?)\s*</div>', chunk, re.DOTALL)
    badge_m = re.search(r'class="badge-psa">\s*(.*?)\s*</span>', chunk)
    size_m = re.search(r'class="badge-size">\s*(.*?)\s*</span>', chunk)
    links = re.findall(r'<a[^>]+href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', chunk, re.DOTALL)
    
    title = title_m.group(1).strip() if title_m else ""
    badge = badge_m.group(1).strip() if badge_m else ""
    size = size_m.group(1).strip() if size_m else ""
    
    s_m = re.search(r'S0?(\d+)', title, re.IGNORECASE)
    e_m = re.search(r'E0?(\d+)|Episode-0?(\d+)', f"{title} {badge}", re.IGNORECASE)
    s_num = int(s_m.group(1)) if s_m else 1
    e_num = int(e_m.group(1) or e_m.group(2)) if e_m else 1
    
    clean_links = [(h, re.sub(r'<[^>]+>', '', t).strip()) for h, t in links if 'hub' in h or 'green' in h]
    ep_data.append((s_num, e_num, size, len(clean_links), title[:60]))

print("\nParsed Sample Episodes:")
for s, e, sz, nlinks, t in ep_data[:15]:
    print(f"Season {s} Ep {e:02d} | Size: {sz} | Links: {nlinks} | {t}")

print(f"Total episodes parsed: {len(ep_data)}")
seasons = set(x[0] for x in ep_data)
for s in sorted(seasons):
    eps_in_s = sorted(set(x[1] for x in ep_data if x[0] == s))
    print(f"Season {s}: Episodes {eps_in_s}")
